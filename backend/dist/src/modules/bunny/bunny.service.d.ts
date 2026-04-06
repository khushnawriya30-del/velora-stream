import { Model } from 'mongoose';
import { MovieDocument } from '../../schemas/movie.schema';
import { SeasonDocument, EpisodeDocument } from '../../schemas/series.schema';
export interface StreamProgress {
    jobId: string;
    type: 'import' | 'migrate' | 'series-import' | 'bulk-upload';
    label: string;
    total: number;
    uploaded: number;
    transcoding: number;
    completed: number;
    failed: number;
    current: string[];
    running: boolean;
    startedAt: number;
    results: {
        id: string;
        title: string;
        status: string;
        error?: string;
        bunnyVideoId?: string;
    }[];
}
export interface BunnyVideo {
    videoLibraryId: number;
    guid: string;
    title: string;
    status: number;
    encodeProgress: number;
    availableResolutions: string;
    width: number;
    height: number;
    length: number;
    storageSize: number;
    thumbnailFileName: string;
}
export declare class BunnyService {
    private movieModel;
    private seasonModel;
    private episodeModel;
    private readonly logger;
    private readonly workerUrl;
    private jobs;
    private readonly MAX_JOBS_HISTORY;
    private driveAccessToken;
    private driveTokenExpiry;
    private serviceAccountKey;
    constructor(movieModel: Model<MovieDocument>, seasonModel: Model<SeasonDocument>, episodeModel: Model<EpisodeDocument>);
    private loadServiceAccount;
    private getDriveAccessToken;
    private listDriveFolderViaApi;
    private downloadDriveFileViaApi;
    hlsUrl(videoId: string): string;
    thumbnailUrl(videoId: string): string;
    mp4Url(videoId: string, resolution: string): string;
    directPlayUrl(videoId: string): string;
    private streamApi;
    createVideo(title: string, collectionId?: string): Promise<BunnyVideo>;
    fetchVideoFromUrl(title: string, url: string, collectionId?: string): Promise<BunnyVideo>;
    getVideoStatus(videoId: string): Promise<BunnyVideo>;
    listVideos(page?: number, perPage?: number, search?: string): Promise<{
        totalItems: number;
        items: BunnyVideo[];
    }>;
    deleteVideo(videoId: string): Promise<void>;
    uploadVideoBinary(videoId: string, fileBuffer: Buffer): Promise<void>;
    downloadAndUploadToBunny(title: string, sourceUrl: string, collectionId?: string, driveFileId?: string): Promise<BunnyVideo>;
    private streamTooBunnyUpload;
    cleanupFailedVideos(): Promise<{
        deleted: number;
        errors: number;
    }>;
    createCollection(name: string): Promise<{
        guid: string;
        name: string;
    }>;
    listCollections(): Promise<{
        totalItems: number;
        items: {
            guid: string;
            name: string;
            videoCount: number;
        }[];
    }>;
    listVideosInCollection(collectionId: string, page?: number, perPage?: number): Promise<{
        totalItems: number;
        items: BunnyVideo[];
    }>;
    importFromBunnyCollection(seasonId: string, collectionId: string): Promise<{
        imported: number;
        skipped: number;
        episodes: {
            episodeNumber: number;
            title: string;
            videoId: string;
            status: string;
        }[];
    }>;
    importSeriesFromBunnyCollection(seriesId: string, collectionId: string): Promise<{
        seasons: {
            seasonNumber: number;
            imported: number;
            episodes: {
                episodeNumber: number;
                title: string;
                videoId: string;
                status: string;
            }[];
        }[];
        totalImported: number;
    }>;
    private parseVideoPath;
    private extractEpisodeFromPath;
    private cleanEpisodeTitleFromPath;
    previewBunnyCollectionStructure(collectionId: string): Promise<{
        collectionName: string;
        seasons: {
            seasonNumber: number;
            episodes: {
                episodeNumber: number;
                title: string;
                videoId: string;
                size: number;
                duration: number;
            }[];
        }[];
    }>;
    importMovieFromBunnyVideo(videoId: string, titleOverride?: string, existingMovieId?: string): Promise<{
        movieId: string;
        title: string;
        hlsUrl: string;
        status: string;
        streamingSources: any[];
    }>;
    private runParallel;
    private createJob;
    getProgress(jobId?: string): StreamProgress | StreamProgress[] | null;
    getActiveJobs(): StreamProgress[];
    uploadMovieFromUrl(movieId: string, sourceUrl?: string): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    uploadMovieFromFile(movieId: string, fileBuffer: Buffer, filename: string): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    checkMovieTranscoding(movieId: string): Promise<{
        status: number;
        encodeProgress: number;
        hlsStatus: string;
        availableResolutions: string;
    }>;
    uploadEpisodeFromUrl(episodeId: string, sourceUrl?: string, collectionId?: string): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    uploadEpisodeFromFile(episodeId: string, fileBuffer: Buffer, filename: string, collectionId?: string): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    importSeasonFromFolder(seasonId: string, folderUrl: string): Promise<{
        jobId: string;
        message: string;
    }>;
    private runFolderImportParallel;
    migrateSeasonToBunnyStream(seasonId: string): Promise<{
        jobId: string;
        message: string;
    }>;
    private runSeasonMigrationParallel;
    importFullSeries(seriesId: string, folderUrl: string): Promise<{
        jobId: string;
        message: string;
        seasons: number;
        episodes: number;
    }>;
    private runFullSeriesImport;
    private scanDriveFolder;
    private listDriveFolder;
    handleWebhook(body: {
        VideoId: string;
        Status: number;
        VideoLibraryId: number;
    }): Promise<{
        handled: boolean;
    }>;
    bulkUploadEpisodes(seasonId: string, episodes: {
        episodeId: string;
        url: string;
    }[]): Promise<{
        jobId: string;
        message: string;
    }>;
    private runBulkUpload;
    checkSeasonTranscoding(seasonId: string): Promise<{
        total: number;
        finished: number;
        processing: number;
        failed: number;
        episodes: {
            episodeNumber: number;
            status: number;
            encodeProgress: number;
            resolutions: string;
        }[];
    }>;
    getLibraryStatus(): Promise<{
        totalVideos: number;
        videos: BunnyVideo[];
    }>;
    buildStreamingSources(videoId: string, availableResolutions: string): any[];
    private extractVideoIdFromHls;
    private ensureStreamUrl;
    private extractDriveFileId;
    private isVideoFile;
    private extractSeasonNumber;
    private extractEpisodeNumber;
    private cleanEpisodeTitle;
}
