import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Movie, MovieDocument } from '../../schemas/movie.schema';
import { Season, SeasonDocument, Episode, EpisodeDocument } from '../../schemas/series.schema';

// Bunny Stream library details
const LIBRARY_ID = '628904';
const LIBRARY_API_KEY = 'f4b47df9-f70d-4269-a2be2a035a23-a746-4436';
const CDN_HOSTNAME = 'vz-f3b830f6-306.b-cdn.net';
const STREAM_API = `https://video.bunnycdn.com/library/${LIBRARY_ID}`;

export interface StreamProgress {
  total: number;
  completed: number;
  failed: number;
  current: string | null;
  running: boolean;
  results: { id: string; title: string; status: string; error?: string; bunnyVideoId?: string }[];
}

export interface BunnyVideo {
  videoLibraryId: number;
  guid: string;
  title: string;
  status: number; // 0=created, 1=uploaded, 2=processing, 3=transcoding, 4=finished, 5=error, 6=UploadFailed
  encodeProgress: number;
  availableResolutions: string;
  width: number;
  height: number;
  length: number;
  storageSize: number;
  thumbnailFileName: string;
}

@Injectable()
export class BunnyService {
  private readonly logger = new Logger(BunnyService.name);
  private readonly workerUrl = 'https://drive-index.vishunawriya11122.workers.dev';

  private progress: StreamProgress = {
    total: 0,
    completed: 0,
    failed: 0,
    current: null,
    running: false,
    results: [],
  };

  constructor(
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
    @InjectModel(Season.name) private seasonModel: Model<SeasonDocument>,
    @InjectModel(Episode.name) private episodeModel: Model<EpisodeDocument>,
  ) {}

  // ─── URL Builders ────────────────────────────────────────────

  hlsUrl(videoId: string): string {
    return `https://${CDN_HOSTNAME}/${videoId}/playlist.m3u8`;
  }

  thumbnailUrl(videoId: string): string {
    return `https://${CDN_HOSTNAME}/${videoId}/thumbnail.jpg`;
  }

  mp4Url(videoId: string, resolution: string): string {
    return `https://${CDN_HOSTNAME}/${videoId}/play_${resolution}.mp4`;
  }

  directPlayUrl(videoId: string): string {
    return `https://iframe.mediadelivery.net/embed/${LIBRARY_ID}/${videoId}`;
  }

  // ─── Bunny Stream API Helpers ────────────────────────────────

  private async streamApi<T = any>(
    path: string,
    method: string = 'GET',
    body?: any,
  ): Promise<T> {
    const url = `${STREAM_API}${path}`;
    const headers: Record<string, string> = {
      AccessKey: LIBRARY_API_KEY,
      accept: 'application/json',
    };
    const opts: RequestInit = { method, headers };
    if (body) {
      headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    const res = await fetch(url, opts);
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Bunny Stream API ${method} ${path}: ${res.status} ${text}`);
    }
    const text = await res.text();
    return text ? JSON.parse(text) : ({} as T);
  }

  // ─── Video CRUD ──────────────────────────────────────────────

  /** Create a blank video entry (to upload binary later) */
  async createVideo(title: string, collectionId?: string): Promise<BunnyVideo> {
    const body: any = { title };
    if (collectionId) body.collectionId = collectionId;
    return this.streamApi<BunnyVideo>('/videos', 'POST', body);
  }

  /** Fetch a video from a URL — Bunny downloads & auto-transcodes */
  async fetchVideoFromUrl(title: string, url: string, collectionId?: string): Promise<BunnyVideo> {
    const body: any = { url, title };
    if (collectionId) body.collectionId = collectionId;
    return this.streamApi<BunnyVideo>('/videos/fetch', 'POST', body);
  }

  /** Get video status (encoding progress, available resolutions) */
  async getVideoStatus(videoId: string): Promise<BunnyVideo> {
    return this.streamApi<BunnyVideo>(`/videos/${videoId}`);
  }

  /** List all videos in the library */
  async listVideos(page = 1, perPage = 100, search?: string): Promise<{ totalItems: number; items: BunnyVideo[] }> {
    let path = `/videos?page=${page}&itemsPerPage=${perPage}&orderBy=date`;
    if (search) path += `&search=${encodeURIComponent(search)}`;
    return this.streamApi(path);
  }

  /** Delete a video from Bunny Stream */
  async deleteVideo(videoId: string): Promise<void> {
    await this.streamApi(`/videos/${videoId}`, 'DELETE');
  }

  /** Upload binary video data to an existing video entry */
  async uploadVideoBinary(videoId: string, fileBuffer: Buffer): Promise<void> {
    const url = `${STREAM_API}/videos/${videoId}`;
    const res = await fetch(url, {
      method: 'PUT',
      headers: {
        AccessKey: LIBRARY_API_KEY,
        'Content-Type': 'application/octet-stream',
      },
      body: new Uint8Array(fileBuffer),
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Upload binary failed: ${res.status} ${text}`);
    }
  }

  // ─── Collections ─────────────────────────────────────────────

  async createCollection(name: string): Promise<{ guid: string; name: string }> {
    return this.streamApi('/collections', 'POST', { name });
  }

  async listCollections(): Promise<{ totalItems: number; items: { guid: string; name: string; videoCount: number }[] }> {
    return this.streamApi('/collections');
  }

  // ─── Progress Tracking ───────────────────────────────────────

  getProgress(): StreamProgress {
    return { ...this.progress };
  }

  // ─── Movie Upload to Bunny Stream ───────────────────────────

  /**
   * Upload a movie to Bunny Stream by fetching from an existing URL.
   * Sets hlsUrl + hlsStatus on the movie, plus streaming sources for each resolution.
   */
  async uploadMovieFromUrl(movieId: string, sourceUrl?: string): Promise<{ bunnyVideoId: string; hlsUrl: string }> {
    const movie = await this.movieModel.findById(movieId);
    if (!movie) throw new Error('Movie not found');

    // Use provided URL or first streaming source
    let url = sourceUrl;
    if (!url && movie.streamingSources?.length) {
      url = movie.streamingSources[0].url;
    }
    if (!url) throw new Error('No video URL available for this movie');

    // If URL is a Drive/Worker URL, ensure it goes through the worker
    url = this.ensureStreamUrl(url);

    this.logger.log(`Fetching movie "${movie.title}" to Bunny Stream from: ${url}`);

    const video = await this.fetchVideoFromUrl(movie.title, url);

    // Update movie with HLS URL
    movie.hlsUrl = this.hlsUrl(video.guid);
    movie.hlsStatus = 'processing';
    movie.streamingSources = [
      { label: 'Auto', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 } as any,
    ];
    await movie.save();

    this.logger.log(`Movie "${movie.title}" → Bunny video ${video.guid}, transcoding started`);
    return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
  }

  /**
   * Upload a movie from a direct file buffer (multipart upload from admin).
   */
  async uploadMovieFromFile(movieId: string, fileBuffer: Buffer, filename: string): Promise<{ bunnyVideoId: string; hlsUrl: string }> {
    const movie = await this.movieModel.findById(movieId);
    if (!movie) throw new Error('Movie not found');

    const video = await this.createVideo(movie.title);
    await this.uploadVideoBinary(video.guid, fileBuffer);

    movie.hlsUrl = this.hlsUrl(video.guid);
    movie.hlsStatus = 'processing';
    movie.streamingSources = [
      { label: 'Auto', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 } as any,
    ];
    await movie.save();

    this.logger.log(`Movie "${movie.title}" uploaded as ${video.guid}`);
    return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
  }

  /** Check transcoding status for a movie and update hlsStatus */
  async checkMovieTranscoding(movieId: string): Promise<{
    status: number;
    encodeProgress: number;
    hlsStatus: string;
    availableResolutions: string;
  }> {
    const movie = await this.movieModel.findById(movieId);
    if (!movie) throw new Error('Movie not found');
    if (!movie.hlsUrl) throw new Error('Movie has no Bunny Stream video');

    const videoId = this.extractVideoIdFromHls(movie.hlsUrl);
    const video = await this.getVideoStatus(videoId);

    let hlsStatus = movie.hlsStatus;
    if (video.status === 4) {
      hlsStatus = 'completed';
      // Add per-resolution streaming sources
      movie.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
    } else if (video.status === 5 || video.status === 6) {
      hlsStatus = 'failed';
    } else if (video.status >= 1 && video.status <= 3) {
      hlsStatus = 'processing';
    }

    if (hlsStatus !== movie.hlsStatus) {
      movie.hlsStatus = hlsStatus;
      await movie.save();
    }

    return {
      status: video.status,
      encodeProgress: video.encodeProgress,
      hlsStatus,
      availableResolutions: video.availableResolutions,
    };
  }

  // ─── Episode Upload to Bunny Stream ──────────────────────────

  /**
   * Upload a single episode to Bunny Stream from a URL.
   */
  async uploadEpisodeFromUrl(
    episodeId: string,
    sourceUrl?: string,
    collectionId?: string,
  ): Promise<{ bunnyVideoId: string; hlsUrl: string }> {
    const episode = await this.episodeModel.findById(episodeId);
    if (!episode) throw new Error('Episode not found');

    let url = sourceUrl;
    if (!url && episode.streamingSources?.length) {
      url = episode.streamingSources[0].url;
    }
    if (!url) throw new Error('No video URL available for this episode');

    url = this.ensureStreamUrl(url);

    const videoTitle = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
    this.logger.log(`Fetching episode "${videoTitle}" to Bunny Stream from: ${url}`);

    const video = await this.fetchVideoFromUrl(videoTitle, url, collectionId);

    episode.streamingSources = [
      { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 } as any,
    ];
    if (!episode.thumbnailUrl) {
      episode.thumbnailUrl = this.thumbnailUrl(video.guid);
    }
    await episode.save();

    return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
  }

  /**
   * Upload episode from file buffer.
   */
  async uploadEpisodeFromFile(
    episodeId: string,
    fileBuffer: Buffer,
    filename: string,
    collectionId?: string,
  ): Promise<{ bunnyVideoId: string; hlsUrl: string }> {
    const episode = await this.episodeModel.findById(episodeId);
    if (!episode) throw new Error('Episode not found');

    const videoTitle = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
    const video = await this.createVideo(videoTitle, collectionId);
    await this.uploadVideoBinary(video.guid, fileBuffer);

    episode.streamingSources = [
      { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 } as any,
    ];
    if (!episode.thumbnailUrl) {
      episode.thumbnailUrl = this.thumbnailUrl(video.guid);
    }
    await episode.save();

    return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
  }

  // ─── Season / Folder Import ──────────────────────────────────

  /**
   * Import all episodes from a Google Drive folder into Bunny Stream.
   * Auto-detects episode files, creates episodes, and starts transcoding.
   * Runs in background — poll getProgress() for status.
   */
  async importSeasonFromFolder(
    seasonId: string,
    folderUrl: string,
  ): Promise<{ message: string }> {
    if (this.progress.running) {
      throw new Error('An import is already in progress');
    }

    const season = await this.seasonModel.findById(seasonId);
    if (!season) throw new Error('Season not found');

    // Extract folder ID
    const folderIdMatch = folderUrl.match(/folders?\/([\w-]+)/);
    if (!folderIdMatch) throw new Error('Invalid Google Drive folder URL');
    const folderId = folderIdMatch[1];

    // List folder via Worker
    const listUrl = `${this.workerUrl}/list?id=${folderId}`;
    const listRes = await fetch(listUrl);
    if (!listRes.ok) throw new Error(`Folder scan failed: ${listRes.status}`);
    const files: { id: string; name: string; mimeType: string }[] = await listRes.json() as any;

    // Filter to video files
    const videoFiles = files.filter(
      (f) => f.mimeType?.startsWith('video/') || /\.(mp4|mkv|avi|webm|mov)$/i.test(f.name),
    );
    videoFiles.sort((a, b) => this.extractEpisodeNumber(a.name) - this.extractEpisodeNumber(b.name));

    if (!videoFiles.length) throw new Error('No video files found in folder');

    // Create a collection for this season
    const series = await this.movieModel.findById(season.seriesId);
    const collectionName = `${series?.title || 'Series'} - Season ${season.seasonNumber}`;
    let collectionId: string | undefined;
    try {
      const col = await this.createCollection(collectionName);
      collectionId = col.guid;
    } catch (err) {
      this.logger.warn(`Could not create collection: ${err.message}`);
    }

    // Initialize progress
    this.progress = {
      total: videoFiles.length,
      completed: 0,
      failed: 0,
      current: null,
      running: true,
      results: [],
    };

    // Run import in background
    this.runFolderImport(seasonId, videoFiles, collectionId).catch((err) => {
      this.logger.error(`Folder import crashed: ${err.message}`);
      this.progress.running = false;
    });

    return { message: `Importing ${videoFiles.length} episodes to Bunny Stream` };
  }

  private async runFolderImport(
    seasonId: string,
    videoFiles: { id: string; name: string; mimeType: string }[],
    collectionId?: string,
  ): Promise<void> {
    // Get existing episodes for this season
    const existingEpisodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
    const existingNumbers = new Set(existingEpisodes.map((e) => e.episodeNumber));

    for (let i = 0; i < videoFiles.length; i++) {
      const file = videoFiles[i];
      const epNum = this.extractEpisodeNumber(file.name) || (i + 1);
      const epTitle = this.cleanEpisodeTitle(file.name, epNum);
      this.progress.current = `Episode ${epNum}: ${file.name}`;

      try {
        // Build stream URL from Drive file ID
        const streamUrl = `${this.workerUrl}/stream/${file.id}`;

        // Fetch to Bunny Stream (Bunny downloads from URL)
        const video = await this.fetchVideoFromUrl(
          `${epTitle} - Episode ${epNum}`,
          streamUrl,
          collectionId,
        );

        const hlsLink = this.hlsUrl(video.guid);
        const thumb = this.thumbnailUrl(video.guid);

        // Create or update episode in DB
        if (existingNumbers.has(epNum)) {
          // Update existing episode
          await this.episodeModel.findOneAndUpdate(
            { seasonId, episodeNumber: epNum },
            {
              streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
              thumbnailUrl: thumb,
            },
          );
        } else {
          // Create new episode
          await this.episodeModel.create({
            seasonId,
            episodeNumber: epNum,
            title: epTitle,
            streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
            thumbnailUrl: thumb,
          });
        }

        // Update season episode count
        const epCount = await this.episodeModel.countDocuments({ seasonId });
        await this.seasonModel.findByIdAndUpdate(seasonId, { episodeCount: epCount });

        this.progress.completed++;
        this.progress.results.push({
          id: video.guid,
          title: `Episode ${epNum}`,
          status: 'success',
          bunnyVideoId: video.guid,
        });

        this.logger.log(`Episode ${epNum} → Bunny video ${video.guid}`);
      } catch (err) {
        this.progress.failed++;
        this.progress.completed++;
        this.progress.results.push({
          id: file.id,
          title: `Episode ${epNum}`,
          status: 'failed',
          error: err.message,
        });
        this.logger.error(`Episode ${epNum} failed: ${err.message}`);
      }
    }

    this.progress.current = null;
    this.progress.running = false;
    this.logger.log(
      `Folder import complete: ${this.progress.completed - this.progress.failed}/${this.progress.total} succeeded`,
    );
  }

  /**
   * Migrate existing season episodes (that already have CDN/Worker URLs) to Bunny Stream.
   * Re-fetches each episode's video to Bunny for native HLS transcoding.
   */
  async migrateSeasonToBunnyStream(seasonId: string): Promise<{ message: string }> {
    if (this.progress.running) {
      throw new Error('An operation is already in progress');
    }

    const episodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
    if (!episodes.length) throw new Error('No episodes found for this season');

    const season = await this.seasonModel.findById(seasonId);
    const series = season ? await this.movieModel.findById(season.seriesId) : null;
    const collectionName = `${series?.title || 'Series'} - Season ${season?.seasonNumber || '?'}`;

    let collectionId: string | undefined;
    try {
      const col = await this.createCollection(collectionName);
      collectionId = col.guid;
    } catch (err) {
      this.logger.warn(`Could not create collection: ${err.message}`);
    }

    this.progress = {
      total: episodes.length,
      completed: 0,
      failed: 0,
      current: null,
      running: true,
      results: [],
    };

    this.runSeasonMigration(episodes, collectionId).catch((err) => {
      this.logger.error(`Season migration crashed: ${err.message}`);
      this.progress.running = false;
    });

    return { message: `Migrating ${episodes.length} episodes to Bunny Stream` };
  }

  private async runSeasonMigration(
    episodes: EpisodeDocument[],
    collectionId?: string,
  ): Promise<void> {
    for (const episode of episodes) {
      const epLabel = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
      this.progress.current = epLabel;

      try {
        let sourceUrl = episode.streamingSources?.[0]?.url;
        if (!sourceUrl) {
          throw new Error('No source URL');
        }
        sourceUrl = this.ensureStreamUrl(sourceUrl);

        const video = await this.fetchVideoFromUrl(epLabel, sourceUrl, collectionId);

        episode.streamingSources = [
          { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 } as any,
        ];
        if (!episode.thumbnailUrl || episode.thumbnailUrl.includes('drive.google.com')) {
          episode.thumbnailUrl = this.thumbnailUrl(video.guid);
        }
        await episode.save();

        this.progress.completed++;
        this.progress.results.push({
          id: episode._id.toString(),
          title: epLabel,
          status: 'success',
          bunnyVideoId: video.guid,
        });
      } catch (err) {
        this.progress.failed++;
        this.progress.completed++;
        this.progress.results.push({
          id: episode._id.toString(),
          title: epLabel,
          status: 'failed',
          error: err.message,
        });
      }
    }

    this.progress.current = null;
    this.progress.running = false;
  }

  /**
   * Check transcoding status for all episodes in a season.
   */
  async checkSeasonTranscoding(seasonId: string): Promise<{
    total: number;
    finished: number;
    processing: number;
    failed: number;
    episodes: { episodeNumber: number; status: number; encodeProgress: number; resolutions: string }[];
  }> {
    const episodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
    let finished = 0;
    let processing = 0;
    let failed = 0;
    const episodeStatuses = [];

    for (const ep of episodes) {
      const hlsSource = ep.streamingSources?.find((s) => s.url?.includes('playlist.m3u8'));
      if (!hlsSource) {
        episodeStatuses.push({ episodeNumber: ep.episodeNumber, status: -1, encodeProgress: 0, resolutions: '' });
        continue;
      }

      const videoId = this.extractVideoIdFromHls(hlsSource.url);
      try {
        const video = await this.getVideoStatus(videoId);
        episodeStatuses.push({
          episodeNumber: ep.episodeNumber,
          status: video.status,
          encodeProgress: video.encodeProgress,
          resolutions: video.availableResolutions,
        });

        if (video.status === 4) {
          finished++;
          // Update sources with per-resolution MP4s
          ep.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
          await ep.save();
        } else if (video.status === 5 || video.status === 6) {
          failed++;
        } else {
          processing++;
        }
      } catch (err) {
        episodeStatuses.push({ episodeNumber: ep.episodeNumber, status: -1, encodeProgress: 0, resolutions: '' });
        failed++;
      }
    }

    return { total: episodes.length, finished, processing, failed, episodes: episodeStatuses };
  }

  /** Get library overview stats */
  async getLibraryStatus(): Promise<{
    totalVideos: number;
    videos: BunnyVideo[];
  }> {
    const data = await this.listVideos(1, 100);
    return { totalVideos: data.totalItems, videos: data.items };
  }

  // ─── Utility Methods ─────────────────────────────────────────

  /** Build streaming sources array from available resolutions */
  private buildStreamingSources(videoId: string, availableResolutions: string): any[] {
    const sources: any[] = [
      { label: 'Auto (HLS)', url: this.hlsUrl(videoId), quality: 'auto', priority: 0 },
    ];
    if (availableResolutions) {
      const resolutions = availableResolutions.split(',').map((r) => r.trim()).filter(Boolean);
      // Sort by resolution descending
      const resPriority: Record<string, number> = { '1080p': 1, '720p': 2, '480p': 3, '360p': 4, '240p': 5 };
      resolutions.sort((a, b) => (resPriority[a] || 99) - (resPriority[b] || 99));
      for (const res of resolutions) {
        sources.push({
          label: res,
          url: this.mp4Url(videoId, res.replace('p', '')),
          quality: res,
          priority: resPriority[res] || 99,
        });
      }
    }
    return sources;
  }

  /** Extract Bunny video GUID from HLS URL */
  private extractVideoIdFromHls(hlsUrl: string): string {
    // https://vz-xxx.b-cdn.net/VIDEO_ID/playlist.m3u8
    const match = hlsUrl.match(/\/([a-f0-9-]+)\/playlist\.m3u8/);
    if (!match) throw new Error(`Cannot extract video ID from: ${hlsUrl}`);
    return match[1];
  }

  /** Ensure a URL is a streamable Worker URL */
  private ensureStreamUrl(url: string): string {
    // Already a direct/CDN URL — return as-is
    if (!url.includes('drive.google.com') && !url.includes('drive.usercontent.google.com')) {
      return url;
    }
    // Extract Drive file ID and convert to Worker stream URL
    const fileId = this.extractDriveFileId(url);
    if (fileId) {
      return `${this.workerUrl}/stream/${fileId}`;
    }
    return url;
  }

  /** Extract Google Drive file ID from any URL format */
  private extractDriveFileId(url: string): string | null {
    if (!url) return null;
    const patterns = [
      /drive\.google\.com\/file\/d\/([a-zA-Z0-9_-]+)/,
      /drive\.google\.com\/open\?id=([a-zA-Z0-9_-]+)/,
      /drive\.google\.com\/uc\?.*id=([a-zA-Z0-9_-]+)/,
      /drive\.usercontent\.google\.com\/.*[?&]id=([a-zA-Z0-9_-]+)/,
      /\/stream\/([a-zA-Z0-9_-]+)/,
    ];
    for (const pattern of patterns) {
      const match = url.match(pattern);
      if (match) return match[1];
    }
    return null;
  }

  /** Extract episode number from filename */
  private extractEpisodeNumber(filename: string): number {
    const match = filename.match(/(?:ep|episode|e)[\s._-]*(\d+)/i) || filename.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : 0;
  }

  /** Clean episode title from filename */
  private cleanEpisodeTitle(filename: string, epNum: number): string {
    // Remove extension
    let name = filename.replace(/\.[^.]+$/, '');
    // Remove common prefixes
    name = name.replace(/^(EP|Episode|E)[\s._-]*\d+[\s._-]*/i, '');
    // Clean underscores/dots
    name = name.replace(/[._]/g, ' ').trim();
    return name || `Episode ${epNum}`;
  }
}
