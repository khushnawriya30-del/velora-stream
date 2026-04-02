import { BunnyService } from './bunny.service';
export declare class BunnyController {
    private readonly bunnyService;
    constructor(bunnyService: BunnyService);
    handleWebhook(body: {
        VideoId: string;
        Status: number;
        VideoLibraryId: number;
    }): Promise<{
        handled: boolean;
    }>;
    getLibraryStatus(): Promise<{
        totalVideos: number;
        videos: import("./bunny.service").BunnyVideo[];
    }>;
    listVideos(): Promise<{
        totalItems: number;
        items: import("./bunny.service").BunnyVideo[];
    }>;
    getVideoStatus(videoId: string): Promise<import("./bunny.service").BunnyVideo>;
    cleanupFailedVideos(): Promise<{
        deleted: number;
        errors: number;
    }>;
    getActiveJobs(): import("./bunny.service").StreamProgress[];
    getProgress(jobId?: string): import("./bunny.service").StreamProgress | import("./bunny.service").StreamProgress[] | null;
    fetchMovieFromUrl(movieId: string, body: {
        url?: string;
    }): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    uploadMovieFile(movieId: string, file: Express.Multer.File): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    checkMovieTranscoding(movieId: string): Promise<{
        status: number;
        encodeProgress: number;
        hlsStatus: string;
        availableResolutions: string;
    }>;
    fetchEpisodeFromUrl(episodeId: string, body: {
        url?: string;
    }): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    uploadEpisodeFile(episodeId: string, file: Express.Multer.File): Promise<{
        bunnyVideoId: string;
        hlsUrl: string;
    }>;
    importSeasonFromFolder(seasonId: string, body: {
        folderUrl: string;
    }): Promise<{
        jobId: string;
        message: string;
    }>;
    migrateSeasonToBunnyStream(seasonId: string): Promise<{
        jobId: string;
        message: string;
    }>;
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
    bulkUploadEpisodes(seasonId: string, body: {
        episodes: {
            episodeId: string;
            url: string;
        }[];
    }): Promise<{
        jobId: string;
        message: string;
    }>;
    importFullSeries(seriesId: string, body: {
        folderUrl: string;
    }): Promise<{
        jobId: string;
        message: string;
        seasons: number;
        episodes: number;
    }>;
    listCollections(): Promise<{
        totalItems: number;
        items: {
            guid: string;
            name: string;
            videoCount: number;
        }[];
    }>;
    listVideosInCollection(collectionId: string): Promise<{
        totalItems: number;
        items: import("./bunny.service").BunnyVideo[];
    }>;
    importFromBunnyCollection(seasonId: string, body: {
        collectionId: string;
    }): Promise<{
        imported: number;
        skipped: number;
        episodes: {
            episodeNumber: number;
            title: string;
            videoId: string;
            status: string;
        }[];
    }>;
    importMovieFromBunnyCollection(body: {
        videoId: string;
        collectionId: string;
        title?: string;
        existingMovieId?: string;
    }): Promise<{
        movieId: string;
        title: string;
        hlsUrl: string;
        status: string;
        streamingSources: any[];
    }>;
}
