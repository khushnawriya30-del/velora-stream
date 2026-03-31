import { Injectable, BadRequestException, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { SeriesService } from '../series/series.service';
import { Types } from 'mongoose';

interface DriveFile {
  id: string;
  name: string;
  mimeType: string;
}

export interface ScannedEpisode {
  fileId: string;
  fileName: string;
  episodeNumber: number;
  title: string;
  streamUrl: string;
  thumbnailUrl: string;
}

export interface ScannedSeason {
  seasonNumber: number;
  folderName?: string;
  folderId?: string;
  episodes: ScannedEpisode[];
}

export interface ScanResult {
  totalFiles: number;
  seasons: ScannedSeason[];
}

const VIDEO_EXTENSIONS = /\.(mp4|mkv|avi|mov|webm|ts|m4v|flv|wmv|3gp)$/i;
const VIDEO_MIMES = ['video/mp4', 'video/x-matroska', 'video/avi', 'video/quicktime', 'video/webm', 'video/mp2t'];

@Injectable()
export class GdriveFolderService {
  private readonly logger = new Logger(GdriveFolderService.name);

  constructor(
    private readonly configService: ConfigService,
    private readonly seriesService: SeriesService,
  ) {}

  /* ================================================================
   *  PUBLIC METHODS
   * ================================================================ */

  async scanFolder(folderUrl: string): Promise<ScanResult> {
    const folderId = this.extractFolderId(folderUrl);
    const apiKey = this.configService.get<string>('GOOGLE_API_KEY');

    const topLevel = apiKey
      ? await this.listViaApi(folderId, apiKey)
      : await this.listViaScrape(folderId);

    if (topLevel.length === 0) {
      throw new BadRequestException(
        'No files found. Make sure the folder is shared as "Anyone with the link".',
      );
    }

    const folders = topLevel.filter((f) => f.mimeType === 'application/vnd.google-apps.folder');
    const videos = topLevel.filter((f) => this.isVideo(f));

    const seasons: ScannedSeason[] = [];

    // Subfolders → treat each as a season
    if (folders.length > 0) {
      const sorted = [...folders].sort((a, b) => {
        const na = this.extractSeasonNumber(a.name) ?? Infinity;
        const nb = this.extractSeasonNumber(b.name) ?? Infinity;
        return na - nb || a.name.localeCompare(b.name);
      });

      for (let i = 0; i < sorted.length; i++) {
        const folder = sorted[i];
        const seasonNum = this.extractSeasonNumber(folder.name) ?? i + 1;

        const subFiles = apiKey
          ? await this.listViaApi(folder.id, apiKey)
          : await this.listViaScrape(folder.id);

        const subVideos = subFiles.filter((f) => this.isVideo(f));
        if (subVideos.length > 0) {
          seasons.push({
            seasonNumber: seasonNum,
            folderName: folder.name,
            folderId: folder.id,
            episodes: this.buildEpisodes(subVideos),
          });
        }
      }
    }

    // Root-level videos → single season
    if (videos.length > 0) {
      const seasonNum = seasons.length > 0 ? Math.max(...seasons.map((s) => s.seasonNumber)) + 1 : 1;
      seasons.push({
        seasonNumber: seasonNum,
        episodes: this.buildEpisodes(videos),
      });
    }

    return {
      totalFiles: seasons.reduce((sum, s) => sum + s.episodes.length, 0),
      seasons,
    };
  }

  async importToSeries(
    seriesId: string,
    scanResult: ScanResult,
  ): Promise<{ seasonsCreated: number; episodesCreated: number }> {
    let seasonsCreated = 0;
    let episodesCreated = 0;

    for (const scanned of scanResult.seasons) {
      const season = await this.seriesService.createSeason({
        seriesId: new Types.ObjectId(seriesId) as any,
        seasonNumber: scanned.seasonNumber,
        title: scanned.folderName || `Season ${scanned.seasonNumber}`,
      });
      seasonsCreated++;

      const episodes = scanned.episodes.map((ep) => ({
        episodeNumber: ep.episodeNumber,
        title: ep.title,
        streamingSources: [{ quality: 'original', url: ep.streamUrl, label: 'Google Drive', priority: 0 }],
      }));

      await this.seriesService.createBulkEpisodes(season._id.toString(), episodes);
      episodesCreated += episodes.length;
    }

    return { seasonsCreated, episodesCreated };
  }

  /* ================================================================
   *  GOOGLE DRIVE v3 REST API (simple API key — works for ALL public folders)
   * ================================================================ */

  private async listViaApi(folderId: string, apiKey: string): Promise<DriveFile[]> {
    const files: DriveFile[] = [];
    let pageToken: string | undefined;

    do {
      const params = new URLSearchParams({
        q: `'${folderId}' in parents and trashed = false`,
        key: apiKey,
        fields: 'nextPageToken,files(id,name,mimeType)',
        pageSize: '1000',
        orderBy: 'name',
      });
      if (pageToken) params.set('pageToken', pageToken);

      const res = await fetch(
        `https://www.googleapis.com/drive/v3/files?${params.toString()}`,
      );

      if (!res.ok) {
        const text = await res.text();
        this.logger.error(`Drive API error: ${res.status} ${text}`);
        throw new BadRequestException(
          'Failed to list folder via Drive API. Ensure the folder is public.',
        );
      }

      const data = await res.json();
      for (const f of data.files || []) {
        files.push({ id: f.id, name: f.name, mimeType: f.mimeType });
      }
      pageToken = data.nextPageToken;
    } while (pageToken);

    return files;
  }

  /* ================================================================
   *  HTML SCRAPING FALLBACK (no API key needed — best effort)
   * ================================================================ */

  private async listViaScrape(folderId: string): Promise<DriveFile[]> {
    const url = `https://drive.google.com/drive/folders/${folderId}`;
    const res = await fetch(url, {
      headers: {
        'User-Agent':
          'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        Accept:
          'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.5',
      },
      redirect: 'follow',
    });

    if (!res.ok) {
      throw new BadRequestException(
        `Could not access folder (HTTP ${res.status}). Ensure the folder is shared as "Anyone with the link".`,
      );
    }

    const html = await res.text();
    return this.parseHtml(html);
  }

  private parseHtml(html: string): DriveFile[] {
    // Unescape hex- and unicode-encoded chars that Google embeds in <script> tags
    const unescaped = html
      .replace(/\\x([\da-fA-F]{2})/g, (_, h) =>
        String.fromCharCode(parseInt(h, 16)),
      )
      .replace(/\\u([\da-fA-F]{4})/g, (_, h) =>
        String.fromCharCode(parseInt(h, 16)),
      );

    const files: DriveFile[] = [];
    const seen = new Set<string>();

    // Pattern 1: ["FILE_ID",["FILENAME"]]
    const p1 = /\["([\w-]{25,})",\["([^"]+)"\]/g;
    let m: RegExpExecArray | null;
    while ((m = p1.exec(unescaped)) !== null) {
      if (!seen.has(m[1])) {
        seen.add(m[1]);
        files.push({
          id: m[1],
          name: m[2],
          mimeType: this.guessMime(m[2]),
        });
      }
    }

    // Pattern 2: Broader — ["FILE_ID","FILENAME"]
    if (files.length === 0) {
      const p2 = /\["([\w-]{25,})","([^"]+)"/g;
      while ((m = p2.exec(unescaped)) !== null) {
        if (
          !seen.has(m[1]) &&
          !m[2].startsWith('http') &&
          m[2].length < 200
        ) {
          seen.add(m[1]);
          files.push({
            id: m[1],
            name: m[2],
            mimeType: this.guessMime(m[2]),
          });
        }
      }
    }

    // Pattern 3: data-id attribute (embedded folder view)
    if (files.length === 0) {
      const p3 = /data-id="([\w-]{25,})"[^>]*>[\s\S]*?class="[^"]*entry-title[^"]*"[^>]*>([^<]+)</g;
      while ((m = p3.exec(html)) !== null) {
        if (!seen.has(m[1])) {
          seen.add(m[1]);
          files.push({
            id: m[1],
            name: m[2].trim(),
            mimeType: this.guessMime(m[2].trim()),
          });
        }
      }
    }

    return files;
  }

  /* ================================================================
   *  HELPERS
   * ================================================================ */

  private extractFolderId(url: string): string {
    const m1 = url.match(/drive\.google\.com\/drive\/folders\/([\w-]+)/);
    if (m1) return m1[1];
    const m2 = url.match(/[?&]id=([\w-]+)/);
    if (m2) return m2[1];
    // If they just pasted an ID directly
    if (/^[\w-]{20,}$/.test(url.trim())) return url.trim();
    throw new BadRequestException(
      'Invalid Google Drive folder URL. Paste the link from "Share → Copy link".',
    );
  }

  private isVideo(f: DriveFile): boolean {
    if (f.mimeType.startsWith('video/')) return true;
    if (VIDEO_MIMES.includes(f.mimeType)) return true;
    return VIDEO_EXTENSIONS.test(f.name);
  }

  private guessMime(name: string): string {
    if (/\.mp4$/i.test(name)) return 'video/mp4';
    if (/\.mkv$/i.test(name)) return 'video/x-matroska';
    if (/\.avi$/i.test(name)) return 'video/avi';
    if (/\.mov$/i.test(name)) return 'video/quicktime';
    if (/\.webm$/i.test(name)) return 'video/webm';
    if (/\.ts$/i.test(name)) return 'video/mp2t';
    if (/\.m4v$/i.test(name)) return 'video/mp4';
    // No extension or non-video → could be a folder
    if (!/\.\w+$/.test(name)) return 'application/vnd.google-apps.folder';
    return 'application/octet-stream';
  }

  private extractSeasonNumber(name: string): number | null {
    // "Season 1", "S01", "S1", "Season01", "season-1"
    const m = name.match(/(?:season|s)\s*[-_.]?\s*(\d+)/i);
    if (m) return parseInt(m[1], 10);
    // Bare number: "1", "01", "02"
    const m2 = name.match(/^(\d{1,2})$/);
    if (m2) return parseInt(m2[1], 10);
    return null;
  }

  private extractEpisodeNumber(name: string): number | null {
    // "S01E05", "s1e5"
    const m1 = name.match(/[Ss]\d+\s*[Ee](\d+)/);
    if (m1) return parseInt(m1[1], 10);
    // "E05", "EP05", "Episode 5", "Ep.5", "Ep 05"
    const m2 = name.match(/(?:^|[^a-z])e(?:p(?:isode)?)?[\s._-]*(\d+)/i);
    if (m2) return parseInt(m2[1], 10);
    // "- 05 -", "- 05.", "05."  (standalone 2-digit number)
    const m3 = name.match(/(?:^|[\s._-])(\d{1,3})(?:[\s._-]|$)/);
    if (m3) return parseInt(m3[1], 10);
    return null;
  }

  private cleanEpisodeTitle(name: string): string {
    // Remove file extension
    let title = name.replace(/\.\w{2,4}$/, '');
    // Remove common patterns like [1080p], (720p), etc.
    title = title.replace(/[\[(]\d{3,4}p[\])]/gi, '');
    // Remove leading "S01E01 -" style prefixes
    title = title.replace(/^[Ss]\d+[Ee]\d+\s*[-_.]\s*/, '');
    // Remove leading episode numbers "01 -", "E01 -"
    title = title.replace(/^[Ee]?[Pp]?\d{1,3}\s*[-_.]\s*/, '');
    // Clean up remaining separators
    title = title.replace(/[._]/g, ' ').trim();
    return title || name.replace(/\.\w{2,4}$/, '');
  }

  private buildEpisodes(files: DriveFile[]): ScannedEpisode[] {
    // Sort files by extracted episode number, then by name
    const withNumbers = files.map((f) => ({
      file: f,
      epNum: this.extractEpisodeNumber(f.name),
    }));

    withNumbers.sort((a, b) => {
      if (a.epNum !== null && b.epNum !== null) return a.epNum - b.epNum;
      if (a.epNum !== null) return -1;
      if (b.epNum !== null) return 1;
      return a.file.name.localeCompare(b.file.name);
    });

    let nextNumber = 1;
    return withNumbers.map(({ file, epNum }) => {
      const episodeNumber = epNum ?? nextNumber;
      nextNumber = episodeNumber + 1;

      const cleaned = this.cleanEpisodeTitle(file.name);
      const title = cleaned || `Episode ${episodeNumber}`;
      const driveUrl = `https://drive.google.com/file/d/${file.id}/view`;

      return {
        fileId: file.id,
        fileName: file.name,
        episodeNumber,
        title,
        streamUrl: driveUrl,
        thumbnailUrl: `https://drive.google.com/thumbnail?id=${file.id}&sz=w800`,
      };
    });
  }
}
