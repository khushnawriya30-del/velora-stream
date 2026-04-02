"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var GdriveFolderService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.GdriveFolderService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const mongoose_1 = require("@nestjs/mongoose");
const mongoose_2 = require("mongoose");
const series_service_1 = require("../series/series.service");
const movie_schema_1 = require("../../schemas/movie.schema");
const mongoose_3 = require("mongoose");
const VIDEO_EXTENSIONS = /\.(mp4|mkv|avi|mov|webm|ts|m4v|flv|wmv|3gp)$/i;
const VIDEO_MIMES = ['video/mp4', 'video/x-matroska', 'video/avi', 'video/quicktime', 'video/webm', 'video/mp2t'];
let GdriveFolderService = GdriveFolderService_1 = class GdriveFolderService {
    constructor(configService, seriesService, movieModel) {
        this.configService = configService;
        this.seriesService = seriesService;
        this.movieModel = movieModel;
        this.logger = new common_1.Logger(GdriveFolderService_1.name);
    }
    async scanFolder(folderUrl) {
        const folderId = this.extractFolderId(folderUrl);
        const workerUrl = this.configService.get('DRIVE_WORKER_URL');
        const apiKey = this.configService.get('GOOGLE_API_KEY');
        let topLevel;
        if (workerUrl) {
            topLevel = await this.listViaWorker(folderId, workerUrl);
        }
        else if (apiKey) {
            topLevel = await this.listViaApi(folderId, apiKey);
        }
        else {
            topLevel = await this.listViaScrape(folderId);
        }
        if (topLevel.length === 0) {
            throw new common_1.BadRequestException('No files found. Make sure the folder is shared as "Anyone with the link".');
        }
        const folders = topLevel.filter((f) => f.mimeType === 'application/vnd.google-apps.folder');
        const videos = topLevel.filter((f) => this.isVideo(f));
        const seasons = [];
        if (folders.length > 0) {
            const sorted = [...folders].sort((a, b) => {
                const na = this.extractSeasonNumber(a.name) ?? Infinity;
                const nb = this.extractSeasonNumber(b.name) ?? Infinity;
                return na - nb || a.name.localeCompare(b.name);
            });
            for (let i = 0; i < sorted.length; i++) {
                const folder = sorted[i];
                const seasonNum = this.extractSeasonNumber(folder.name) ?? i + 1;
                let subFiles;
                if (workerUrl) {
                    subFiles = await this.listViaWorker(folder.id, workerUrl);
                }
                else if (apiKey) {
                    subFiles = await this.listViaApi(folder.id, apiKey);
                }
                else {
                    subFiles = await this.listViaScrape(folder.id);
                }
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
    async importToSeries(seriesId, scanResult, driveFolderUrl) {
        let seasonsCreated = 0;
        let episodesCreated = 0;
        const workerUrl = this.configService.get('DRIVE_WORKER_URL');
        for (const scanned of scanResult.seasons) {
            const season = await this.seriesService.createSeason({
                seriesId: new mongoose_3.Types.ObjectId(seriesId),
                seasonNumber: scanned.seasonNumber,
                title: scanned.folderName || `Season ${scanned.seasonNumber}`,
            });
            seasonsCreated++;
            const episodes = scanned.episodes.map((ep) => ({
                episodeNumber: ep.episodeNumber,
                title: ep.title,
                streamingSources: [{
                        quality: 'original',
                        url: workerUrl
                            ? `${workerUrl}/stream/${ep.fileId}`
                            : ep.streamUrl,
                        label: 'Google Drive',
                        priority: 0,
                    }],
                thumbnailUrl: ep.thumbnailUrl,
            }));
            await this.seriesService.createBulkEpisodes(season._id.toString(), episodes);
            episodesCreated += episodes.length;
        }
        if (driveFolderUrl) {
            await this.movieModel.findByIdAndUpdate(seriesId, { driveFolderUrl });
        }
        return { seasonsCreated, episodesCreated };
    }
    async refreshFromDrive(seriesId) {
        const movie = await this.movieModel.findById(seriesId).lean();
        if (!movie?.driveFolderUrl) {
            throw new common_1.BadRequestException('No Drive folder linked to this series.');
        }
        const scan = await this.scanFolder(movie.driveFolderUrl);
        const existingSeasons = await this.seriesService.getSeasons(seriesId);
        const workerUrl = this.configService.get('DRIVE_WORKER_URL');
        let newEpisodes = 0;
        for (const scanned of scan.seasons) {
            let season = existingSeasons.find((s) => s.seasonNumber === scanned.seasonNumber);
            if (!season) {
                season = await this.seriesService.createSeason({
                    seriesId: new mongoose_3.Types.ObjectId(seriesId),
                    seasonNumber: scanned.seasonNumber,
                    title: scanned.folderName || `Season ${scanned.seasonNumber}`,
                });
            }
            const existingEps = await this.seriesService.getEpisodes(season._id.toString());
            const existingNums = new Set(existingEps.map((e) => e.episodeNumber));
            const newEps = scanned.episodes
                .filter((ep) => !existingNums.has(ep.episodeNumber))
                .map((ep) => ({
                episodeNumber: ep.episodeNumber,
                title: ep.title,
                streamingSources: [{
                        quality: 'original',
                        url: workerUrl
                            ? `${workerUrl}/stream/${ep.fileId}`
                            : ep.streamUrl,
                        label: 'Google Drive',
                        priority: 0,
                    }],
                thumbnailUrl: ep.thumbnailUrl,
            }));
            if (newEps.length > 0) {
                await this.seriesService.createBulkEpisodes(season._id.toString(), newEps);
                newEpisodes += newEps.length;
            }
        }
        return { newEpisodes };
    }
    async listViaWorker(folderId, workerUrl) {
        const url = `${workerUrl}/list?id=${folderId}`;
        const res = await fetch(url);
        if (!res.ok) {
            const text = await res.text();
            this.logger.error(`Worker error: ${res.status} ${text}`);
            throw new common_1.BadRequestException('Drive Worker failed to list folder. Ensure the folder is public.');
        }
        const data = (await res.json());
        if (Array.isArray(data) && 'error' in data) {
            throw new common_1.BadRequestException(data.error);
        }
        return data.map((f) => ({ id: f.id, name: f.name, mimeType: f.mimeType }));
    }
    async listViaApi(folderId, apiKey) {
        const files = [];
        let pageToken;
        do {
            const params = new URLSearchParams({
                q: `'${folderId}' in parents and trashed = false`,
                key: apiKey,
                fields: 'nextPageToken,files(id,name,mimeType)',
                pageSize: '1000',
                orderBy: 'name',
            });
            if (pageToken)
                params.set('pageToken', pageToken);
            const res = await fetch(`https://www.googleapis.com/drive/v3/files?${params.toString()}`);
            if (!res.ok) {
                const text = await res.text();
                this.logger.error(`Drive API error: ${res.status} ${text}`);
                throw new common_1.BadRequestException('Failed to list folder via Drive API. Ensure the folder is public.');
            }
            const data = await res.json();
            for (const f of data.files || []) {
                files.push({ id: f.id, name: f.name, mimeType: f.mimeType });
            }
            pageToken = data.nextPageToken;
        } while (pageToken);
        return files;
    }
    async listViaScrape(folderId) {
        try {
            const url = `https://drive.google.com/embeddedfolderview?id=${folderId}#list`;
            const res = await fetch(url, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
                    Accept: 'text/html',
                },
                redirect: 'follow',
            });
            if (res.ok) {
                const html = await res.text();
                const files = this.parseEmbeddedHtml(html, folderId);
                if (files.length > 0) {
                    this.logger.log(`Layer 1 (embedded view) → ${files.length} items`);
                    return files;
                }
            }
            else {
                this.logger.warn(`Layer 1 HTTP ${res.status}`);
            }
        }
        catch (e) {
            this.logger.warn(`Layer 1 failed: ${e.message}`);
        }
        try {
            const url = `https://drive.google.com/drive/folders/${folderId}?usp=sharing`;
            const res = await fetch(url, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
                    Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                    'Accept-Language': 'en-US,en;q=0.5',
                    'Cache-Control': 'no-cache',
                },
                redirect: 'follow',
            });
            if (res.ok) {
                const html = await res.text();
                const files = this.parseHtml(html);
                if (files.length > 0) {
                    this.logger.log(`Layer 2 (standard page) → ${files.length} items`);
                    return files;
                }
            }
            else {
                this.logger.warn(`Layer 2 HTTP ${res.status}`);
            }
        }
        catch (e) {
            this.logger.warn(`Layer 2 failed: ${e.message}`);
        }
        try {
            const url = `https://drive.google.com/drive/u/0/folders/${folderId}`;
            const res = await fetch(url, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36',
                    Accept: 'text/html',
                    'Accept-Language': 'en',
                    'Sec-Fetch-Dest': 'document',
                    'Sec-Fetch-Mode': 'navigate',
                    'Sec-Fetch-Site': 'none',
                },
                redirect: 'follow',
            });
            if (res.ok) {
                const html = await res.text();
                const files = this.parseHtml(html);
                if (files.length > 0) {
                    this.logger.log(`Layer 3 (u/0 mobile) → ${files.length} items`);
                    return files;
                }
            }
            else {
                this.logger.warn(`Layer 3 HTTP ${res.status}`);
            }
        }
        catch (e) {
            this.logger.warn(`Layer 3 failed: ${e.message}`);
        }
        throw new common_1.BadRequestException('Could not list folder contents (all 3 layers failed). Ensure the folder is shared as "Anyone with the link".');
    }
    parseEmbeddedHtml(html, parentFolderId) {
        const files = [];
        const seen = new Set();
        let m;
        const fileRe = /\/file\/d\/([\w-]+)\/[\s\S]*?class="flip-entry-title">([^<]+)<\/div>/gi;
        while ((m = fileRe.exec(html)) !== null) {
            const id = m[1];
            const name = m[2].trim();
            if (!seen.has(id)) {
                seen.add(id);
                files.push({ id, name, mimeType: this.guessMime(name) });
            }
        }
        const folderRe = /\/folders\/([\w-]+)[\s\S]*?class="flip-entry-title">([^<]+)<\/div>/gi;
        while ((m = folderRe.exec(html)) !== null) {
            const id = m[1];
            const name = m[2].trim();
            if (!seen.has(id) && id !== parentFolderId) {
                seen.add(id);
                files.push({ id, name, mimeType: 'application/vnd.google-apps.folder' });
            }
        }
        if (files.length === 0) {
            const entryRe = /id="entry-([\w-]+)"[\s\S]*?class="flip-entry-title">([^<]+)<\/div>/gi;
            while ((m = entryRe.exec(html)) !== null) {
                const id = m[1];
                const name = m[2].trim();
                if (!seen.has(id) && id !== parentFolderId) {
                    seen.add(id);
                    files.push({ id, name, mimeType: this.guessMime(name) });
                }
            }
        }
        return files;
    }
    parseHtml(html) {
        const unescaped = html
            .replace(/\\x([\da-fA-F]{2})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
            .replace(/\\u([\da-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)));
        const files = [];
        const seen = new Set();
        let m;
        const p1 = /\["([\w-]{25,})",\["([^"]+)"\]/g;
        while ((m = p1.exec(unescaped)) !== null) {
            if (!seen.has(m[1])) {
                seen.add(m[1]);
                files.push({ id: m[1], name: m[2], mimeType: this.guessMime(m[2]) });
            }
        }
        if (files.length === 0) {
            const p2 = /\["([\w-]{25,})","([^"]+)"/g;
            while ((m = p2.exec(unescaped)) !== null) {
                if (!seen.has(m[1]) && !m[2].startsWith('http') && m[2].length < 200) {
                    seen.add(m[1]);
                    files.push({ id: m[1], name: m[2], mimeType: this.guessMime(m[2]) });
                }
            }
        }
        if (files.length === 0) {
            const p3 = /data-id="([\w-]{25,})"[^>]*>[\s\S]*?class="[^"]*entry-title[^"]*"[^>]*>([^<]+)/g;
            while ((m = p3.exec(html)) !== null) {
                if (!seen.has(m[1])) {
                    seen.add(m[1]);
                    files.push({ id: m[1], name: m[2].trim(), mimeType: this.guessMime(m[2].trim()) });
                }
            }
        }
        if (files.length === 0) {
            const initRe = /AF_initDataCallback\(\{[^}]*data:([\s\S]*?)\}\s*\)\s*;/g;
            while ((m = initRe.exec(html)) !== null) {
                const blob = m[1];
                const idNameRe = /"([\w-]{25,})"[\s,]*"([^"]{1,200})"/g;
                let sm;
                while ((sm = idNameRe.exec(blob)) !== null) {
                    if (!seen.has(sm[1]) && !sm[2].startsWith('http')) {
                        seen.add(sm[1]);
                        files.push({ id: sm[1], name: sm[2], mimeType: this.guessMime(sm[2]) });
                    }
                }
            }
        }
        if (files.length === 0) {
            const hrefFileRe = /href="[^"]*\/file\/d\/([\w-]+)\/[^"]*"[^>]*>\s*([^<]+)/gi;
            while ((m = hrefFileRe.exec(html)) !== null) {
                if (!seen.has(m[1])) {
                    seen.add(m[1]);
                    files.push({ id: m[1], name: m[2].trim(), mimeType: this.guessMime(m[2].trim()) });
                }
            }
            const hrefFolderRe = /href="[^"]*\/folders\/([\w-]+)[^"]*"[^>]*>\s*([^<]+)/gi;
            while ((m = hrefFolderRe.exec(html)) !== null) {
                if (!seen.has(m[1])) {
                    seen.add(m[1]);
                    files.push({ id: m[1], name: m[2].trim(), mimeType: 'application/vnd.google-apps.folder' });
                }
            }
        }
        return files;
    }
    extractFolderId(url) {
        const m1 = url.match(/drive\.google\.com\/drive\/folders\/([\w-]+)/);
        if (m1)
            return m1[1];
        const m2 = url.match(/[?&]id=([\w-]+)/);
        if (m2)
            return m2[1];
        if (/^[\w-]{20,}$/.test(url.trim()))
            return url.trim();
        throw new common_1.BadRequestException('Invalid Google Drive folder URL. Paste the link from "Share → Copy link".');
    }
    isVideo(f) {
        if (f.mimeType.startsWith('video/'))
            return true;
        if (VIDEO_MIMES.includes(f.mimeType))
            return true;
        return VIDEO_EXTENSIONS.test(f.name);
    }
    guessMime(name) {
        if (/\.mp4$/i.test(name))
            return 'video/mp4';
        if (/\.mkv$/i.test(name))
            return 'video/x-matroska';
        if (/\.avi$/i.test(name))
            return 'video/avi';
        if (/\.mov$/i.test(name))
            return 'video/quicktime';
        if (/\.webm$/i.test(name))
            return 'video/webm';
        if (/\.ts$/i.test(name))
            return 'video/mp2t';
        if (/\.m4v$/i.test(name))
            return 'video/mp4';
        if (!/\.\w+$/.test(name))
            return 'application/vnd.google-apps.folder';
        return 'application/octet-stream';
    }
    extractSeasonNumber(name) {
        const m = name.match(/(?:season|s)\s*[-_.]?\s*(\d+)/i);
        if (m)
            return parseInt(m[1], 10);
        const m2 = name.match(/^(\d{1,2})$/);
        if (m2)
            return parseInt(m2[1], 10);
        return null;
    }
    extractEpisodeNumber(name) {
        const m1 = name.match(/[Ss]\d+\s*[Ee](\d+)/);
        if (m1)
            return parseInt(m1[1], 10);
        const m2 = name.match(/(?:^|[^a-z])e(?:p(?:isode)?)?[\s._-]*(\d+)/i);
        if (m2)
            return parseInt(m2[1], 10);
        const m3 = name.match(/(?:^|[\s._-])(\d{1,3})(?:[\s._-]|$)/);
        if (m3)
            return parseInt(m3[1], 10);
        return null;
    }
    cleanEpisodeTitle(name) {
        let title = name.replace(/\.\w{2,4}$/, '');
        title = title.replace(/[\[(]\d{3,4}p[\])]/gi, '');
        title = title.replace(/^[Ss]\d+[Ee]\d+\s*[-_.]\s*/, '');
        title = title.replace(/^[Ee]?[Pp]?\d{1,3}\s*[-_.]\s*/, '');
        title = title.replace(/[._]/g, ' ').trim();
        return title || name.replace(/\.\w{2,4}$/, '');
    }
    buildEpisodes(files) {
        const withNumbers = files.map((f) => ({
            file: f,
            epNum: this.extractEpisodeNumber(f.name),
        }));
        withNumbers.sort((a, b) => {
            if (a.epNum !== null && b.epNum !== null)
                return a.epNum - b.epNum;
            if (a.epNum !== null)
                return -1;
            if (b.epNum !== null)
                return 1;
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
};
exports.GdriveFolderService = GdriveFolderService;
exports.GdriveFolderService = GdriveFolderService = GdriveFolderService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(2, (0, mongoose_1.InjectModel)(movie_schema_1.Movie.name)),
    __metadata("design:paramtypes", [config_1.ConfigService,
        series_service_1.SeriesService,
        mongoose_2.Model])
], GdriveFolderService);
//# sourceMappingURL=gdrive-folder.service.js.map