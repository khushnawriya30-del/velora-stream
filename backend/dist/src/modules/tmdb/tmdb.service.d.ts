import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
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
    private accessToken;
    constructor(movieModel: Model<MovieDocument>, configService: ConfigService);
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
    private fetchAndCreateMovie;
}
