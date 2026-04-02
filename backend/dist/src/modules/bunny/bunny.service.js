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
var BunnyService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.BunnyService = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const mongoose_2 = require("mongoose");
const movie_schema_1 = require("../../schemas/movie.schema");
const series_schema_1 = require("../../schemas/series.schema");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const LIBRARY_ID = '628904';
const LIBRARY_API_KEY = 'f4b47df9-f70d-4269-a2be2a035a23-a746-4436';
const CDN_HOSTNAME = 'vz-f3b830f6-306.b-cdn.net';
const STREAM_API = `https://video.bunnycdn.com/library/${LIBRARY_ID}`;
const PARALLEL_CONCURRENCY = 3;
let BunnyService = BunnyService_1 = class BunnyService {
    constructor(movieModel, seasonModel, episodeModel) {
        this.movieModel = movieModel;
        this.seasonModel = seasonModel;
        this.episodeModel = episodeModel;
        this.logger = new common_1.Logger(BunnyService_1.name);
        this.workerUrl = 'https://drive-index.vishunawriya11122.workers.dev';
        this.jobs = new Map();
        this.MAX_JOBS_HISTORY = 20;
        this.driveAccessToken = null;
        this.driveTokenExpiry = 0;
        this.serviceAccountKey = null;
        this.loadServiceAccount();
    }
    loadServiceAccount() {
        try {
            const possiblePaths = [
                path.join(process.cwd(), 'gcp-service-account.json'),
                path.join(__dirname, '..', '..', '..', 'gcp-service-account.json'),
                path.join(__dirname, '..', '..', '..', '..', 'gcp-service-account.json'),
            ];
            for (const p of possiblePaths) {
                if (fs.existsSync(p)) {
                    this.serviceAccountKey = JSON.parse(fs.readFileSync(p, 'utf8'));
                    this.logger.log(`Loaded GCP service account from: ${p}`);
                    return;
                }
            }
            this.logger.warn('GCP service account key not found — Drive API auth disabled, will fall back to worker');
        }
        catch (err) {
            this.logger.warn(`Failed to load GCP service account: ${err.message}`);
        }
    }
    async getDriveAccessToken() {
        if (this.driveAccessToken && Date.now() < this.driveTokenExpiry - 300000) {
            return this.driveAccessToken;
        }
        if (!this.serviceAccountKey) {
            throw new Error('No GCP service account key loaded');
        }
        const now = Math.floor(Date.now() / 1000);
        const payload = {
            iss: this.serviceAccountKey.client_email,
            scope: 'https://www.googleapis.com/auth/drive.readonly',
            aud: 'https://oauth2.googleapis.com/token',
            iat: now,
            exp: now + 3600,
        };
        const header = Buffer.from(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).toString('base64url');
        const body = Buffer.from(JSON.stringify(payload)).toString('base64url');
        const signingInput = `${header}.${body}`;
        const signer = crypto.createSign('RSA-SHA256');
        signer.update(signingInput);
        const signature = signer.sign(this.serviceAccountKey.private_key, 'base64url');
        const jwt = `${signingInput}.${signature}`;
        const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`,
        });
        if (!tokenRes.ok) {
            const errText = await tokenRes.text();
            throw new Error(`Google token exchange failed: ${tokenRes.status} ${errText}`);
        }
        const tokenData = (await tokenRes.json());
        this.driveAccessToken = tokenData.access_token;
        this.driveTokenExpiry = Date.now() + tokenData.expires_in * 1000;
        this.logger.log('Google Drive API access token obtained');
        return this.driveAccessToken;
    }
    async listDriveFolderViaApi(folderId) {
        const token = await this.getDriveAccessToken();
        const allFiles = [];
        let pageToken;
        do {
            let url = `https://www.googleapis.com/drive/v3/files?q='${folderId}'+in+parents+and+trashed=false&fields=files(id,name,mimeType,size),nextPageToken&pageSize=1000`;
            if (pageToken)
                url += `&pageToken=${pageToken}`;
            const res = await fetch(url, {
                headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) {
                const errText = await res.text();
                throw new Error(`Drive API list failed: ${res.status} ${errText}`);
            }
            const data = (await res.json());
            allFiles.push(...(data.files || []));
            pageToken = data.nextPageToken;
        } while (pageToken);
        return allFiles;
    }
    async downloadDriveFileViaApi(fileId) {
        const token = await this.getDriveAccessToken();
        const url = `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`;
        const res = await fetch(url, {
            headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) {
            const errText = await res.text();
            throw new Error(`Drive API download failed for ${fileId}: ${res.status} ${errText}`);
        }
        return res;
    }
    hlsUrl(videoId) {
        return `https://${CDN_HOSTNAME}/${videoId}/playlist.m3u8`;
    }
    thumbnailUrl(videoId) {
        return `https://${CDN_HOSTNAME}/${videoId}/thumbnail.jpg`;
    }
    mp4Url(videoId, resolution) {
        return `https://${CDN_HOSTNAME}/${videoId}/play_${resolution}.mp4`;
    }
    directPlayUrl(videoId) {
        return `https://iframe.mediadelivery.net/embed/${LIBRARY_ID}/${videoId}`;
    }
    async streamApi(path, method = 'GET', body) {
        const url = `${STREAM_API}${path}`;
        const headers = {
            AccessKey: LIBRARY_API_KEY,
            accept: 'application/json',
        };
        const opts = { method, headers };
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
        return text ? JSON.parse(text) : {};
    }
    async createVideo(title, collectionId) {
        const body = { title };
        if (collectionId)
            body.collectionId = collectionId;
        return this.streamApi('/videos', 'POST', body);
    }
    async fetchVideoFromUrl(title, url, collectionId) {
        const body = { url, title };
        if (collectionId)
            body.collectionId = collectionId;
        return this.streamApi('/videos/fetch', 'POST', body);
    }
    async getVideoStatus(videoId) {
        return this.streamApi(`/videos/${videoId}`);
    }
    async listVideos(page = 1, perPage = 100, search) {
        let path = `/videos?page=${page}&itemsPerPage=${perPage}&orderBy=date`;
        if (search)
            path += `&search=${encodeURIComponent(search)}`;
        return this.streamApi(path);
    }
    async deleteVideo(videoId) {
        await this.streamApi(`/videos/${videoId}`, 'DELETE');
    }
    async uploadVideoBinary(videoId, fileBuffer) {
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
    async downloadAndUploadToBunny(title, sourceUrl, collectionId, driveFileId) {
        const video = await this.createVideo(title, collectionId);
        try {
            const fileId = driveFileId || this.extractDriveFileId(sourceUrl);
            if (fileId && this.serviceAccountKey) {
                this.logger.log(`[Stream] Downloading via Drive API: ${fileId}`);
                try {
                    const downloadRes = await this.downloadDriveFileViaApi(fileId);
                    return await this.streamTooBunnyUpload(video.guid, downloadRes, video);
                }
                catch (apiErr) {
                    this.logger.warn(`[Stream] Drive API download failed: ${apiErr.message}, trying fallbacks...`);
                }
            }
            this.logger.log(`[Stream] Downloading (fallback): ${sourceUrl.slice(0, 120)}...`);
            let downloadUrl = sourceUrl;
            if (fileId) {
                downloadUrl = `https://drive.usercontent.google.com/download?id=${fileId}&export=download&confirm=t`;
            }
            const downloadRes = await fetch(downloadUrl, {
                redirect: 'follow',
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' },
            });
            if (!downloadRes.ok || !downloadRes.body) {
                throw new Error(`Download failed: HTTP ${downloadRes.status}`);
            }
            const ct = downloadRes.headers.get('content-type') || '';
            if (ct.includes('text/html')) {
                this.logger.warn(`[Stream] Drive returned HTML, falling back to worker proxy...`);
                const workerUrl = fileId ? `${this.workerUrl}/stream/${fileId}` : sourceUrl;
                const fallbackRes = await fetch(workerUrl, {
                    redirect: 'follow',
                    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' },
                });
                if (!fallbackRes.ok || !fallbackRes.body) {
                    throw new Error(`Worker download also failed: HTTP ${fallbackRes.status}`);
                }
                const fbCt = fallbackRes.headers.get('content-type') || '';
                if (fbCt.includes('text/html')) {
                    throw new Error('Cannot download file — Google Drive returned HTML. Share the folder with the service account or make it public.');
                }
                return await this.streamTooBunnyUpload(video.guid, fallbackRes, video);
            }
            return await this.streamTooBunnyUpload(video.guid, downloadRes, video);
        }
        catch (err) {
            try {
                await this.deleteVideo(video.guid);
            }
            catch { }
            throw err;
        }
    }
    async streamTooBunnyUpload(videoId, downloadRes, video) {
        const uploadUrl = `${STREAM_API}/videos/${videoId}`;
        const uploadRes = await fetch(uploadUrl, {
            method: 'PUT',
            headers: {
                AccessKey: LIBRARY_API_KEY,
                'Content-Type': 'application/octet-stream',
            },
            body: downloadRes.body,
            duplex: 'half',
        });
        if (!uploadRes.ok) {
            const errText = await uploadRes.text();
            throw new Error(`Bunny upload failed: ${uploadRes.status} ${errText}`);
        }
        this.logger.log(`[Stream] Upload complete → Bunny video ${videoId}`);
        return video;
    }
    async cleanupFailedVideos() {
        const allVideos = await this.listVideos(1, 1000);
        const failedVideos = allVideos.items.filter((v) => v.status === 5 || v.status === 6 || (v.status === 0 && v.storageSize === 0 && v.length === 0));
        this.logger.log(`[Cleanup] Found ${failedVideos.length} failed/empty videos to delete`);
        let deleted = 0;
        let errors = 0;
        for (const v of failedVideos) {
            try {
                await this.deleteVideo(v.guid);
                deleted++;
                this.logger.log(`[Cleanup] Deleted ${v.guid} (${v.title})`);
            }
            catch (err) {
                errors++;
                this.logger.error(`[Cleanup] Failed to delete ${v.guid}: ${err.message}`);
            }
        }
        return { deleted, errors };
    }
    async createCollection(name) {
        return this.streamApi('/collections', 'POST', { name });
    }
    async listCollections() {
        return this.streamApi('/collections');
    }
    async listVideosInCollection(collectionId, page = 1, perPage = 100) {
        return this.streamApi(`/videos?page=${page}&itemsPerPage=${perPage}&collection=${collectionId}&orderBy=date`);
    }
    async importFromBunnyCollection(seasonId, collectionId) {
        const season = await this.seasonModel.findById(seasonId);
        if (!season)
            throw new Error('Season not found');
        const allVideos = [];
        let page = 1;
        while (true) {
            const result = await this.listVideosInCollection(collectionId, page, 100);
            allVideos.push(...result.items);
            if (allVideos.length >= result.totalItems)
                break;
            page++;
        }
        if (allVideos.length === 0)
            throw new Error('No videos found in this Bunny collection');
        allVideos.sort((a, b) => this.extractEpisodeNumber(a.title) - this.extractEpisodeNumber(b.title));
        const existingEpisodes = await this.episodeModel.find({ seasonId }).exec();
        const existingByNumber = new Map(existingEpisodes.map((e) => [e.episodeNumber, e]));
        let imported = 0;
        let skipped = 0;
        const results = [];
        for (let i = 0; i < allVideos.length; i++) {
            const video = allVideos[i];
            const epNum = this.extractEpisodeNumber(video.title) || (i + 1);
            const epTitle = this.cleanEpisodeTitle(video.title, epNum);
            const hlsLink = this.hlsUrl(video.guid);
            const thumb = this.thumbnailUrl(video.guid);
            const sources = this.buildStreamingSources(video.guid, video.availableResolutions || '');
            if (existingByNumber.has(epNum)) {
                await this.episodeModel.findOneAndUpdate({ seasonId, episodeNumber: epNum }, { title: epTitle, streamingSources: sources, thumbnailUrl: thumb });
                results.push({ episodeNumber: epNum, title: epTitle, videoId: video.guid, status: 'updated' });
                imported++;
            }
            else {
                await this.episodeModel.create({
                    seasonId: new mongoose_2.Types.ObjectId(seasonId),
                    episodeNumber: epNum,
                    title: epTitle,
                    streamingSources: sources,
                    thumbnailUrl: thumb,
                });
                results.push({ episodeNumber: epNum, title: epTitle, videoId: video.guid, status: 'created' });
                imported++;
            }
        }
        const epCount = await this.episodeModel.countDocuments({ seasonId });
        await this.seasonModel.findByIdAndUpdate(seasonId, { episodeCount: epCount });
        this.logger.log(`[Bunny Import] Season ${season.seasonNumber}: ${imported} episodes imported, ${skipped} skipped`);
        return { imported, skipped, episodes: results };
    }
    async importMovieFromBunnyVideo(videoId, titleOverride, existingMovieId) {
        const video = await this.getVideoStatus(videoId);
        if (!video)
            throw new Error('Video not found in Bunny Stream');
        const title = titleOverride || video.title || 'Untitled Movie';
        const hlsLink = this.hlsUrl(video.guid);
        const thumb = this.thumbnailUrl(video.guid);
        const sources = this.buildStreamingSources(video.guid, video.availableResolutions || '');
        if (existingMovieId) {
            const movie = await this.movieModel.findById(existingMovieId);
            if (!movie)
                throw new Error('Movie not found');
            movie.streamingSources = sources;
            movie.hlsUrl = hlsLink;
            movie.hlsStatus = video.status === 4 ? 'completed' : 'processing';
            if (!movie.posterUrl || movie.posterUrl === '')
                movie.posterUrl = thumb;
            if (!movie.bannerUrl || movie.bannerUrl === '')
                movie.bannerUrl = thumb;
            await movie.save();
            this.logger.log(`[Bunny Movie Import] Linked video to existing movie "${movie.title}" (${movie._id})`);
            return { movieId: movie._id.toString(), title: movie.title, hlsUrl: hlsLink, status: 'linked', streamingSources: sources };
        }
        const existing = await this.movieModel.findOne({ hlsUrl: hlsLink });
        if (existing) {
            existing.streamingSources = sources;
            existing.hlsStatus = video.status === 4 ? 'completed' : 'processing';
            await existing.save();
            this.logger.log(`[Bunny Movie Import] Updated existing movie "${existing.title}" (${existing._id})`);
            return { movieId: existing._id.toString(), title: existing.title, hlsUrl: hlsLink, status: 'updated', streamingSources: sources };
        }
        const movie = await this.movieModel.create({
            title,
            synopsis: `Imported from Bunny Stream: ${title}`,
            posterUrl: thumb,
            bannerUrl: thumb,
            genres: [],
            languages: [],
            releaseYear: new Date().getFullYear(),
            contentType: 'movie',
            status: 'draft',
            streamingSources: sources,
            hlsUrl: hlsLink,
            hlsStatus: video.status === 4 ? 'completed' : 'processing',
            tags: [],
            cast: [],
        });
        this.logger.log(`[Bunny Movie Import] Created movie "${title}" (${movie._id})`);
        return { movieId: movie._id.toString(), title, hlsUrl: hlsLink, status: 'created', streamingSources: sources };
    }
    async runParallel(items, fn, concurrency = PARALLEL_CONCURRENCY) {
        const results = new Array(items.length);
        let succeeded = 0;
        let failed = 0;
        let nextIndex = 0;
        const worker = async () => {
            while (nextIndex < items.length) {
                const idx = nextIndex++;
                try {
                    results[idx] = await fn(items[idx], idx);
                    succeeded++;
                }
                catch (err) {
                    results[idx] = err instanceof Error ? err : new Error(String(err));
                    failed++;
                }
            }
        };
        const workers = Array.from({ length: Math.min(concurrency, items.length) }, () => worker());
        await Promise.all(workers);
        return { results, succeeded, failed };
    }
    createJob(type, label, total) {
        if (this.jobs.size >= this.MAX_JOBS_HISTORY) {
            const finished = [...this.jobs.entries()]
                .filter(([, j]) => !j.running)
                .sort((a, b) => a[1].startedAt - b[1].startedAt);
            for (const [key] of finished.slice(0, finished.length - 5)) {
                this.jobs.delete(key);
            }
        }
        const jobId = `${type}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
        const job = {
            jobId,
            type,
            label,
            total,
            uploaded: 0,
            transcoding: 0,
            completed: 0,
            failed: 0,
            current: [],
            running: true,
            startedAt: Date.now(),
            results: [],
        };
        this.jobs.set(jobId, job);
        return job;
    }
    getProgress(jobId) {
        if (jobId)
            return this.jobs.get(jobId) || null;
        return [...this.jobs.values()].sort((a, b) => b.startedAt - a.startedAt);
    }
    getActiveJobs() {
        return [...this.jobs.values()].filter((j) => j.running);
    }
    async uploadMovieFromUrl(movieId, sourceUrl) {
        const movie = await this.movieModel.findById(movieId);
        if (!movie)
            throw new Error('Movie not found');
        let url = sourceUrl;
        if (!url && movie.streamingSources?.length) {
            url = movie.streamingSources[0].url;
        }
        if (!url)
            throw new Error('No video URL available for this movie');
        url = this.ensureStreamUrl(url);
        this.logger.log(`Uploading movie "${movie.title}" to Bunny Stream from: ${url}`);
        const video = await this.downloadAndUploadToBunny(movie.title, url);
        movie.hlsUrl = this.hlsUrl(video.guid);
        movie.hlsStatus = 'processing';
        movie.streamingSources = [
            { label: 'Auto', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
        ];
        await movie.save();
        this.logger.log(`Movie "${movie.title}" → Bunny video ${video.guid}, transcoding started`);
        return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
    }
    async uploadMovieFromFile(movieId, fileBuffer, filename) {
        const movie = await this.movieModel.findById(movieId);
        if (!movie)
            throw new Error('Movie not found');
        const video = await this.createVideo(movie.title);
        await this.uploadVideoBinary(video.guid, fileBuffer);
        movie.hlsUrl = this.hlsUrl(video.guid);
        movie.hlsStatus = 'processing';
        movie.streamingSources = [
            { label: 'Auto', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
        ];
        await movie.save();
        this.logger.log(`Movie "${movie.title}" uploaded as ${video.guid}`);
        return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
    }
    async checkMovieTranscoding(movieId) {
        const movie = await this.movieModel.findById(movieId);
        if (!movie)
            throw new Error('Movie not found');
        if (!movie.hlsUrl)
            throw new Error('Movie has no Bunny Stream video');
        const videoId = this.extractVideoIdFromHls(movie.hlsUrl);
        const video = await this.getVideoStatus(videoId);
        let hlsStatus = movie.hlsStatus;
        if (video.status === 4) {
            hlsStatus = 'completed';
            movie.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
        }
        else if (video.status === 5 || video.status === 6) {
            hlsStatus = 'failed';
        }
        else if (video.status >= 1 && video.status <= 3) {
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
    async uploadEpisodeFromUrl(episodeId, sourceUrl, collectionId) {
        const episode = await this.episodeModel.findById(episodeId);
        if (!episode)
            throw new Error('Episode not found');
        let url = sourceUrl;
        if (!url && episode.streamingSources?.length) {
            url = episode.streamingSources[0].url;
        }
        if (!url)
            throw new Error('No video URL available for this episode');
        url = this.ensureStreamUrl(url);
        const videoTitle = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
        this.logger.log(`Uploading episode "${videoTitle}" to Bunny Stream from: ${url}`);
        const video = await this.downloadAndUploadToBunny(videoTitle, url, collectionId);
        episode.streamingSources = [
            { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
        ];
        if (!episode.thumbnailUrl) {
            episode.thumbnailUrl = this.thumbnailUrl(video.guid);
        }
        await episode.save();
        return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
    }
    async uploadEpisodeFromFile(episodeId, fileBuffer, filename, collectionId) {
        const episode = await this.episodeModel.findById(episodeId);
        if (!episode)
            throw new Error('Episode not found');
        const videoTitle = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
        const video = await this.createVideo(videoTitle, collectionId);
        await this.uploadVideoBinary(video.guid, fileBuffer);
        episode.streamingSources = [
            { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
        ];
        if (!episode.thumbnailUrl) {
            episode.thumbnailUrl = this.thumbnailUrl(video.guid);
        }
        await episode.save();
        return { bunnyVideoId: video.guid, hlsUrl: this.hlsUrl(video.guid) };
    }
    async importSeasonFromFolder(seasonId, folderUrl) {
        const season = await this.seasonModel.findById(seasonId);
        if (!season)
            throw new Error('Season not found');
        const folderIdMatch = folderUrl.match(/folders?\/([\w-]+)/);
        if (!folderIdMatch)
            throw new Error('Invalid Google Drive folder URL');
        const folderId = folderIdMatch[1];
        const files = await this.listDriveFolder(folderId);
        const videoFiles = files.filter((f) => f.mimeType?.startsWith('video/') || /\.(mp4|mkv|avi|webm|mov)$/i.test(f.name));
        videoFiles.sort((a, b) => this.extractEpisodeNumber(a.name) - this.extractEpisodeNumber(b.name));
        if (!videoFiles.length)
            throw new Error('No video files found in folder');
        const series = await this.movieModel.findById(season.seriesId);
        const collectionName = `${series?.title || 'Series'} - Season ${season.seasonNumber}`;
        let collectionId;
        try {
            const col = await this.createCollection(collectionName);
            collectionId = col.guid;
        }
        catch (err) {
            this.logger.warn(`Could not create collection: ${err.message}`);
        }
        const job = this.createJob('import', `Season ${season.seasonNumber} Import`, videoFiles.length);
        this.runFolderImportParallel(job, seasonId, videoFiles, collectionId).catch((err) => {
            this.logger.error(`Folder import crashed: ${err.message}`);
            job.running = false;
        });
        return { jobId: job.jobId, message: `Importing ${videoFiles.length} episodes in parallel (${PARALLEL_CONCURRENCY} at a time)` };
    }
    async runFolderImportParallel(job, seasonId, videoFiles, collectionId) {
        const existingEpisodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
        const existingNumbers = new Set(existingEpisodes.map((e) => e.episodeNumber));
        await this.runParallel(videoFiles, async (file, index) => {
            const epNum = this.extractEpisodeNumber(file.name) || (index + 1);
            const epTitle = this.cleanEpisodeTitle(file.name, epNum);
            job.current = [...job.current.filter((c) => c !== `Episode ${epNum}`), `Episode ${epNum}`].slice(-PARALLEL_CONCURRENCY);
            try {
                const video = await this.downloadAndUploadToBunny(`${epTitle} - Episode ${epNum}`, '', collectionId, file.id);
                const hlsLink = this.hlsUrl(video.guid);
                const thumb = this.thumbnailUrl(video.guid);
                if (existingNumbers.has(epNum)) {
                    await this.episodeModel.findOneAndUpdate({ seasonId, episodeNumber: epNum }, {
                        streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
                        thumbnailUrl: thumb,
                    });
                }
                else {
                    await this.episodeModel.create({
                        seasonId: new mongoose_2.Types.ObjectId(seasonId),
                        episodeNumber: epNum,
                        title: epTitle,
                        streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
                        thumbnailUrl: thumb,
                    });
                }
                job.uploaded++;
                job.transcoding++;
                job.results.push({ id: video.guid, title: `Episode ${epNum}`, status: 'success', bunnyVideoId: video.guid });
                this.logger.log(`Episode ${epNum} → Bunny video ${video.guid}`);
            }
            catch (err) {
                job.failed++;
                job.results.push({ id: file.id, title: `Episode ${epNum}`, status: 'failed', error: err.message });
                this.logger.error(`Episode ${epNum} failed: ${err.message}`);
            }
        }, PARALLEL_CONCURRENCY);
        const epCount = await this.episodeModel.countDocuments({ seasonId });
        await this.seasonModel.findByIdAndUpdate(seasonId, { episodeCount: epCount });
        job.current = [];
        job.running = false;
        job.completed = job.uploaded;
        this.logger.log(`Folder import complete: ${job.uploaded}/${job.total} succeeded, ${job.failed} failed`);
    }
    async migrateSeasonToBunnyStream(seasonId) {
        const episodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
        if (!episodes.length)
            throw new Error('No episodes found for this season');
        const season = await this.seasonModel.findById(seasonId);
        const series = season ? await this.movieModel.findById(season.seriesId) : null;
        const collectionName = `${series?.title || 'Series'} - Season ${season?.seasonNumber || '?'}`;
        let collectionId;
        try {
            const col = await this.createCollection(collectionName);
            collectionId = col.guid;
        }
        catch (err) {
            this.logger.warn(`Could not create collection: ${err.message}`);
        }
        const job = this.createJob('migrate', `Season ${season?.seasonNumber} Migration`, episodes.length);
        this.runSeasonMigrationParallel(job, episodes, collectionId).catch((err) => {
            this.logger.error(`Season migration crashed: ${err.message}`);
            job.running = false;
        });
        return { jobId: job.jobId, message: `Migrating ${episodes.length} episodes in parallel` };
    }
    async runSeasonMigrationParallel(job, episodes, collectionId) {
        await this.runParallel(episodes, async (episode) => {
            const epLabel = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
            job.current = [...job.current.filter((c) => c !== epLabel), epLabel].slice(-PARALLEL_CONCURRENCY);
            let sourceUrl = episode.streamingSources?.[0]?.url;
            if (!sourceUrl)
                throw new Error('No source URL');
            sourceUrl = this.ensureStreamUrl(sourceUrl);
            const video = await this.downloadAndUploadToBunny(epLabel, sourceUrl, collectionId);
            episode.streamingSources = [
                { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
            ];
            if (!episode.thumbnailUrl || episode.thumbnailUrl.includes('drive.google.com')) {
                episode.thumbnailUrl = this.thumbnailUrl(video.guid);
            }
            await episode.save();
            job.uploaded++;
            job.transcoding++;
            job.results.push({ id: episode._id.toString(), title: epLabel, status: 'success', bunnyVideoId: video.guid });
        }, PARALLEL_CONCURRENCY);
        job.current = [];
        job.running = false;
        job.completed = job.uploaded;
    }
    async importFullSeries(seriesId, folderUrl) {
        const series = await this.movieModel.findById(seriesId);
        if (!series)
            throw new Error('Series not found');
        this.logger.log(`[Full Import] Scanning folder for "${series.title}"...`);
        const scannedSeasons = await this.scanDriveFolder(folderUrl);
        const totalEpisodes = scannedSeasons.reduce((sum, s) => sum + s.episodes.length, 0);
        if (totalEpisodes === 0)
            throw new Error('No video files found in folder');
        const job = this.createJob('series-import', `${series.title} — Full Import`, totalEpisodes);
        this.runFullSeriesImport(job, seriesId, series.title, scannedSeasons).catch((err) => {
            this.logger.error(`Full series import crashed: ${err.message}`);
            job.running = false;
        });
        return {
            jobId: job.jobId,
            message: `Importing ${scannedSeasons.length} season(s) with ${totalEpisodes} episode(s) — parallel upload + auto-transcoding`,
            seasons: scannedSeasons.length,
            episodes: totalEpisodes,
        };
    }
    async runFullSeriesImport(job, seriesId, seriesTitle, scannedSeasons) {
        for (const scanned of scannedSeasons) {
            let season = await this.seasonModel.findOne({
                seriesId: new mongoose_2.Types.ObjectId(seriesId),
                seasonNumber: scanned.seasonNumber,
            });
            if (!season) {
                season = await this.seasonModel.create({
                    seriesId: new mongoose_2.Types.ObjectId(seriesId),
                    seasonNumber: scanned.seasonNumber,
                    title: scanned.folderName || `Season ${scanned.seasonNumber}`,
                    episodeCount: 0,
                });
                this.logger.log(`[Full Import] Created Season ${scanned.seasonNumber}`);
            }
            const seasonId = season._id.toString();
            const collectionName = `${seriesTitle} - Season ${scanned.seasonNumber}`;
            let collectionId;
            try {
                const col = await this.createCollection(collectionName);
                collectionId = col.guid;
            }
            catch (err) {
                this.logger.warn(`Could not create collection: ${err.message}`);
            }
            const existingEpisodes = await this.episodeModel.find({ seasonId: season._id }).exec();
            const existingNumbers = new Set(existingEpisodes.map((e) => e.episodeNumber));
            await this.runParallel(scanned.episodes, async (ep) => {
                const epLabel = `S${scanned.seasonNumber}E${ep.episodeNumber}: ${ep.title}`;
                job.current = [...job.current.filter((c) => c !== epLabel), epLabel].slice(-PARALLEL_CONCURRENCY);
                try {
                    const video = await this.downloadAndUploadToBunny(`${ep.title} - S${scanned.seasonNumber}E${ep.episodeNumber}`, '', collectionId, ep.fileId);
                    const hlsLink = this.hlsUrl(video.guid);
                    const thumb = this.thumbnailUrl(video.guid);
                    if (existingNumbers.has(ep.episodeNumber)) {
                        await this.episodeModel.findOneAndUpdate({ seasonId: season._id, episodeNumber: ep.episodeNumber }, {
                            title: ep.title,
                            streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
                            thumbnailUrl: thumb,
                        });
                    }
                    else {
                        await this.episodeModel.create({
                            seasonId: season._id,
                            episodeNumber: ep.episodeNumber,
                            title: ep.title,
                            streamingSources: [{ label: 'Auto (HLS)', url: hlsLink, quality: 'auto', priority: 0 }],
                            thumbnailUrl: thumb,
                        });
                    }
                    job.uploaded++;
                    job.transcoding++;
                    job.results.push({ id: video.guid, title: epLabel, status: 'success', bunnyVideoId: video.guid });
                    this.logger.log(`${epLabel} → Bunny video ${video.guid}`);
                }
                catch (err) {
                    job.failed++;
                    job.results.push({ id: ep.fileId, title: epLabel, status: 'failed', error: err.message });
                    this.logger.error(`${epLabel} failed: ${err.message}`);
                }
            }, PARALLEL_CONCURRENCY);
            const epCount = await this.episodeModel.countDocuments({ seasonId: season._id });
            await this.seasonModel.findByIdAndUpdate(seasonId, { episodeCount: epCount });
        }
        job.current = [];
        job.running = false;
        job.completed = job.uploaded;
        this.logger.log(`[Full Import] Complete: ${job.uploaded}/${job.total} episodes, ${job.failed} failed`);
    }
    async scanDriveFolder(folderUrl) {
        const folderIdMatch = folderUrl.match(/folders?\/([\w-]+)/);
        if (!folderIdMatch)
            throw new Error('Invalid Google Drive folder URL');
        const folderId = folderIdMatch[1];
        const topLevel = await this.listDriveFolder(folderId);
        const folders = topLevel.filter((f) => f.mimeType === 'application/vnd.google-apps.folder');
        const videos = topLevel.filter((f) => this.isVideoFile(f));
        const seasons = [];
        if (folders.length > 0) {
            const sorted = [...folders].sort((a, b) => {
                const na = this.extractSeasonNumber(a.name);
                const nb = this.extractSeasonNumber(b.name);
                return na - nb || a.name.localeCompare(b.name);
            });
            const folderResults = await this.runParallel(sorted, async (folder, idx) => {
                const seasonNum = this.extractSeasonNumber(folder.name) || (idx + 1);
                const subFiles = await this.listDriveFolder(folder.id);
                const subVideos = subFiles.filter((f) => this.isVideoFile(f));
                if (subVideos.length === 0)
                    return null;
                subVideos.sort((a, b) => this.extractEpisodeNumber(a.name) - this.extractEpisodeNumber(b.name));
                return {
                    seasonNumber: seasonNum,
                    folderName: folder.name,
                    folderId: folder.id,
                    episodes: subVideos.map((v, i) => {
                        const epNum = this.extractEpisodeNumber(v.name) || (i + 1);
                        return {
                            fileId: v.id,
                            fileName: v.name,
                            episodeNumber: epNum,
                            title: this.cleanEpisodeTitle(v.name, epNum),
                        };
                    }),
                };
            }, 5);
            for (const r of folderResults.results) {
                if (r && !(r instanceof Error))
                    seasons.push(r);
            }
        }
        if (videos.length > 0) {
            videos.sort((a, b) => this.extractEpisodeNumber(a.name) - this.extractEpisodeNumber(b.name));
            const seasonNum = seasons.length > 0 ? Math.max(...seasons.map((s) => s.seasonNumber)) + 1 : 1;
            seasons.push({
                seasonNumber: seasonNum,
                episodes: videos.map((v, i) => {
                    const epNum = this.extractEpisodeNumber(v.name) || (i + 1);
                    return {
                        fileId: v.id,
                        fileName: v.name,
                        episodeNumber: epNum,
                        title: this.cleanEpisodeTitle(v.name, epNum),
                    };
                }),
            });
        }
        seasons.sort((a, b) => a.seasonNumber - b.seasonNumber);
        return seasons;
    }
    async listDriveFolder(folderId) {
        if (this.serviceAccountKey) {
            try {
                return await this.listDriveFolderViaApi(folderId);
            }
            catch (err) {
                this.logger.warn(`Drive API list failed: ${err.message}, falling back to worker...`);
            }
        }
        const listUrl = `${this.workerUrl}/list?id=${folderId}`;
        const res = await fetch(listUrl);
        if (!res.ok)
            throw new Error(`Folder scan failed (worker): ${res.status}`);
        return await res.json();
    }
    async handleWebhook(body) {
        const { VideoId: videoId, Status: status } = body;
        if (!videoId)
            return { handled: false };
        this.logger.log(`[Webhook] Video ${videoId} status: ${status}`);
        const hlsUrl = this.hlsUrl(videoId);
        const movie = await this.movieModel.findOne({ hlsUrl });
        if (movie) {
            if (status === 4) {
                movie.hlsStatus = 'completed';
                movie.streamingSources = this.buildStreamingSources(videoId, '');
                try {
                    const video = await this.getVideoStatus(videoId);
                    movie.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
                }
                catch { }
                await movie.save();
                this.logger.log(`[Webhook] Movie "${movie.title}" transcoding completed!`);
            }
            else if (status === 5 || status === 6) {
                movie.hlsStatus = 'failed';
                await movie.save();
                this.logger.log(`[Webhook] Movie "${movie.title}" transcoding FAILED`);
            }
            return { handled: true };
        }
        const episode = await this.episodeModel.findOne({
            'streamingSources.url': hlsUrl,
        });
        if (episode) {
            if (status === 4) {
                try {
                    const video = await this.getVideoStatus(videoId);
                    episode.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
                }
                catch {
                    episode.streamingSources = this.buildStreamingSources(videoId, '');
                }
                await episode.save();
                this.logger.log(`[Webhook] Episode "${episode.title}" transcoding completed!`);
            }
            return { handled: true };
        }
        this.logger.warn(`[Webhook] Video ${videoId} not found in DB`);
        return { handled: false };
    }
    async bulkUploadEpisodes(seasonId, episodes) {
        const season = await this.seasonModel.findById(seasonId);
        if (!season)
            throw new Error('Season not found');
        const series = await this.movieModel.findById(season.seriesId);
        const collectionName = `${series?.title || 'Series'} - Season ${season.seasonNumber}`;
        let collectionId;
        try {
            const col = await this.createCollection(collectionName);
            collectionId = col.guid;
        }
        catch { }
        const job = this.createJob('bulk-upload', `Season ${season.seasonNumber} Bulk Upload`, episodes.length);
        this.runBulkUpload(job, episodes, collectionId).catch((err) => {
            this.logger.error(`Bulk upload crashed: ${err.message}`);
            job.running = false;
        });
        return { jobId: job.jobId, message: `Uploading ${episodes.length} episodes in parallel` };
    }
    async runBulkUpload(job, episodes, collectionId) {
        await this.runParallel(episodes, async ({ episodeId, url }) => {
            const episode = await this.episodeModel.findById(episodeId);
            if (!episode)
                throw new Error(`Episode ${episodeId} not found`);
            const epLabel = `${episode.title || 'Episode'} ${episode.episodeNumber}`;
            job.current = [...job.current.filter((c) => c !== epLabel), epLabel].slice(-PARALLEL_CONCURRENCY);
            const streamUrl = this.ensureStreamUrl(url);
            const video = await this.downloadAndUploadToBunny(epLabel, streamUrl, collectionId);
            episode.streamingSources = [
                { label: 'Auto (HLS)', url: this.hlsUrl(video.guid), quality: 'auto', priority: 0 },
            ];
            if (!episode.thumbnailUrl) {
                episode.thumbnailUrl = this.thumbnailUrl(video.guid);
            }
            await episode.save();
            job.uploaded++;
            job.transcoding++;
            job.results.push({ id: video.guid, title: epLabel, status: 'success', bunnyVideoId: video.guid });
        }, PARALLEL_CONCURRENCY);
        job.current = [];
        job.running = false;
        job.completed = job.uploaded;
    }
    async checkSeasonTranscoding(seasonId) {
        const episodes = await this.episodeModel.find({ seasonId }).sort({ episodeNumber: 1 }).exec();
        let finished = 0;
        let processing = 0;
        let failed = 0;
        const episodeStatuses = [];
        await this.runParallel(episodes, async (ep) => {
            const hlsSource = ep.streamingSources?.find((s) => s.url?.includes('playlist.m3u8'));
            if (!hlsSource) {
                episodeStatuses.push({ episodeNumber: ep.episodeNumber, status: -1, encodeProgress: 0, resolutions: '' });
                return;
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
                    ep.streamingSources = this.buildStreamingSources(videoId, video.availableResolutions);
                    await ep.save();
                }
                else if (video.status === 5 || video.status === 6) {
                    failed++;
                }
                else {
                    processing++;
                }
            }
            catch {
                episodeStatuses.push({ episodeNumber: ep.episodeNumber, status: -1, encodeProgress: 0, resolutions: '' });
                failed++;
            }
        }, 10);
        episodeStatuses.sort((a, b) => a.episodeNumber - b.episodeNumber);
        return { total: episodes.length, finished, processing, failed, episodes: episodeStatuses };
    }
    async getLibraryStatus() {
        const data = await this.listVideos(1, 100);
        return { totalVideos: data.totalItems, videos: data.items };
    }
    buildStreamingSources(videoId, availableResolutions) {
        const sources = [
            { label: 'Auto (HLS)', url: this.hlsUrl(videoId), quality: 'auto', priority: 0 },
        ];
        if (availableResolutions) {
            const resolutions = availableResolutions.split(',').map((r) => r.trim()).filter(Boolean);
            const resPriority = { '2160p': 0, '1440p': 1, '1080p': 2, '720p': 3, '480p': 4, '360p': 5, '240p': 6 };
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
    extractVideoIdFromHls(hlsUrl) {
        const match = hlsUrl.match(/\/([a-f0-9-]+)\/playlist\.m3u8/);
        if (!match)
            throw new Error(`Cannot extract video ID from: ${hlsUrl}`);
        return match[1];
    }
    ensureStreamUrl(url) {
        if (!url.includes('drive.google.com') && !url.includes('drive.usercontent.google.com')) {
            return url;
        }
        const fileId = this.extractDriveFileId(url);
        if (fileId) {
            return `${this.workerUrl}/stream/${fileId}`;
        }
        return url;
    }
    extractDriveFileId(url) {
        if (!url)
            return null;
        const patterns = [
            /drive\.google\.com\/file\/d\/([a-zA-Z0-9_-]+)/,
            /drive\.google\.com\/open\?id=([a-zA-Z0-9_-]+)/,
            /drive\.google\.com\/uc\?.*id=([a-zA-Z0-9_-]+)/,
            /drive\.usercontent\.google\.com\/.*[?&]id=([a-zA-Z0-9_-]+)/,
            /\/stream\/([a-zA-Z0-9_-]+)/,
        ];
        for (const pattern of patterns) {
            const match = url.match(pattern);
            if (match)
                return match[1];
        }
        return null;
    }
    isVideoFile(file) {
        return file.mimeType?.startsWith('video/') || /\.(mp4|mkv|avi|webm|mov|ts|m4v|flv|wmv)$/i.test(file.name);
    }
    extractSeasonNumber(name) {
        const match = name.match(/(?:season|s)[\s._-]*(\d+)/i) || name.match(/(\d+)/);
        return match ? parseInt(match[1], 10) : 0;
    }
    extractEpisodeNumber(filename) {
        const match = filename.match(/(?:ep|episode|e)[\s._-]*(\d+)/i) || filename.match(/(\d+)/);
        return match ? parseInt(match[1], 10) : 0;
    }
    cleanEpisodeTitle(filename, epNum) {
        let name = filename.replace(/\.[^.]+$/, '');
        name = name.replace(/^(EP|Episode|E)[\s._-]*\d+[\s._-]*/i, '');
        name = name.replace(/[._]/g, ' ').trim();
        return name || `Episode ${epNum}`;
    }
};
exports.BunnyService = BunnyService;
exports.BunnyService = BunnyService = BunnyService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, mongoose_1.InjectModel)(movie_schema_1.Movie.name)),
    __param(1, (0, mongoose_1.InjectModel)(series_schema_1.Season.name)),
    __param(2, (0, mongoose_1.InjectModel)(series_schema_1.Episode.name)),
    __metadata("design:paramtypes", [mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model])
], BunnyService);
//# sourceMappingURL=bunny.service.js.map