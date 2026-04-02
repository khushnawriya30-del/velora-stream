import { TmdbService, TmdbDiscoverOptions } from './tmdb.service';
export declare class TmdbController {
    private readonly tmdbService;
    constructor(tmdbService: TmdbService);
    discover(body: TmdbDiscoverOptions): Promise<{
        items: import("./tmdb.service").TmdbPreviewItem[];
        nextPage: number;
    }>;
    searchPerson(body: {
        query: string;
    }): Promise<{
        results: {
            id: number;
            name: string;
            profileUrl: string | null;
            knownFor: string;
        }[];
    }>;
    search(body: {
        query: string;
        contentType: 'movies' | 'shows' | 'anime' | 'webseries';
        page?: number;
    }): Promise<{
        items: import("./tmdb.service").TmdbPreviewItem[];
        nextPage: number;
        totalResults: number;
    }>;
    importItems(body: {
        tmdbIds: number[];
        contentType: 'movies' | 'shows' | 'anime' | 'webseries';
        asUpcoming?: boolean;
    }): Promise<{
        imported: number;
        skipped: number;
        items: any[];
    }>;
}
