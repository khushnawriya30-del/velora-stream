import { Model } from 'mongoose';
import { MovieDocument } from '../../schemas/movie.schema';
export declare class SearchService {
    private movieModel;
    constructor(movieModel: Model<MovieDocument>);
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
    autocomplete(query: string): Promise<any[]>;
    getTrendingSearches(): Promise<string[]>;
    getGenres(): Promise<string[]>;
    getLanguages(): Promise<string[]>;
    getPlatforms(): Promise<string[]>;
    getYears(): Promise<number[]>;
    getRanking(type?: string, contentType?: string, genre?: string, limit?: number): Promise<MovieDocument[]>;
}
