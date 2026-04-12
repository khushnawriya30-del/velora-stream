import { Model } from 'mongoose';
import { MovieDocument } from '../../schemas/movie.schema';
import { SearchQueryDocument } from '../../schemas/search-query.schema';
export declare class SearchService {
    private movieModel;
    private searchQueryModel;
    constructor(movieModel: Model<MovieDocument>, searchQueryModel: Model<SearchQueryDocument>);
    search(query: string, filters?: {
        contentType?: string;
        genre?: string;
        language?: string;
        yearMin?: number;
        yearMax?: number;
        ratingMin?: number;
        sort?: string;
        platform?: string;
    }, page?: number, limit?: number): Promise<{
        results: MovieDocument[];
        total: number;
    }>;
    private trackSearchQuery;
    autocomplete(query: string): Promise<any[]>;
    getTrendingSearches(): Promise<string[]>;
    getMostPopularSearches(): Promise<any[]>;
    getRecommended(limit?: number): Promise<any[]>;
    getGenres(): Promise<string[]>;
    getLanguages(): Promise<string[]>;
    getPlatforms(): Promise<string[]>;
    getYears(): Promise<number[]>;
    getRanking(type?: string, contentType?: string, genre?: string, limit?: number): Promise<MovieDocument[]>;
}
