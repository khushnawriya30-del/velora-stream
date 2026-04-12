import { SearchService } from './search.service';
export declare class SearchController {
    private readonly searchService;
    constructor(searchService: SearchService);
    search(q?: string, contentType?: string, genre?: string, language?: string, yearMin?: number, yearMax?: number, ratingMin?: number, sort?: string, platform?: string, page?: number, limit?: number): Promise<{
        results: import("../../schemas/movie.schema").MovieDocument[];
        total: number;
    }>;
    autocomplete(q: string): Promise<any[]>;
    trending(): Promise<string[]>;
    genres(): Promise<string[]>;
    languages(): Promise<string[]>;
    platforms(): Promise<string[]>;
    years(): Promise<number[]>;
    mostPopular(): Promise<any[]>;
    recommended(limit?: number): Promise<any[]>;
    ranking(type?: string, contentType?: string, genre?: string, limit?: number): Promise<import("../../schemas/movie.schema").MovieDocument[]>;
}
