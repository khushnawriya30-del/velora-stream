import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { SettingsService } from '../settings/settings.service';
import { MovieDocument } from '../../schemas/movie.schema';
export interface TmdbDiscoverOptions {
    contentType: 'movies' | 'shows' | 'anime' | 'webseries';
    region: string;
    count: number;
    year?: number;
    genres?: number[];
    withLanguage?: string;
    withCast?: string;
    page?: number;
    releaseStatus?: 'released' | 'upcoming';
    withWatchProviders?: string;
    watchRegion?: string;
}
export interface TmdbPreviewItem {
    tmdbId: number;
    title: string;
    overview: string;
    posterUrl: string | null;
    backdropUrl: string | null;
    releaseDate: string;
    rating: number;
    genreNames: string[];
    originalLanguage: string;
    alreadyImported: boolean;
}
export declare class TmdbService {
    private movieModel;
    private configService;
    private settingsService;
    private envToken;
    constructor(movieModel: Model<MovieDocument>, configService: ConfigService, settingsService: SettingsService);
    private getToken;
    private tmdbGet;
    discover(opts: TmdbDiscoverOptions): Promise<{
        items: TmdbPreviewItem[];
        nextPage: number;
    }>;
    searchPerson(query: string): Promise<{
        results: {
            id: number;
            name: string;
            profileUrl: string | null;
            knownFor: string;
        }[];
    }>;
    search(opts: {
        query: string;
        contentType: 'movies' | 'shows' | 'anime' | 'webseries';
        page?: number;
        watchProviders?: string;
        watchRegion?: string;
        withOriginalLanguage?: string;
    }): Promise<{
        items: TmdbPreviewItem[];
        nextPage: number;
        totalResults: number;
    }>;
    importItems(tmdbIds: number[], contentType: 'movies' | 'shows' | 'anime' | 'webseries', asUpcoming?: boolean): Promise<{
        imported: number;
        skipped: number;
        items: any[];
    }>;
    private filterByWatchProviders;
    private fetchOttPlatforms;
    private fetchAndCreateMovie;
}
