import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { SeriesService } from '../series/series.service';
import { MovieDocument } from '../../schemas/movie.schema';
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
export declare class GdriveFolderService {
    private readonly configService;
    private readonly seriesService;
    private readonly movieModel;
    private readonly logger;
    constructor(configService: ConfigService, seriesService: SeriesService, movieModel: Model<MovieDocument>);
    scanFolder(folderUrl: string): Promise<ScanResult>;
    importToSeries(seriesId: string, scanResult: ScanResult, driveFolderUrl?: string): Promise<{
        seasonsCreated: number;
        episodesCreated: number;
    }>;
    refreshFromDrive(seriesId: string): Promise<{
        newEpisodes: number;
    }>;
    private listViaWorker;
    private listViaApi;
    private listViaScrape;
    private parseEmbeddedHtml;
    private parseHtml;
    private extractFolderId;
    private isVideo;
    private guessMime;
    private extractSeasonNumber;
    private extractEpisodeNumber;
    private cleanEpisodeTitle;
    private buildEpisodes;
}
