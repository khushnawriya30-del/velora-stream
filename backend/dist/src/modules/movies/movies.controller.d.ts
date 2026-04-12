import { MoviesService } from './movies.service';
import { CreateMovieDto } from './dto/create-movie.dto';
import { UpdateMovieDto } from './dto/update-movie.dto';
import { QueryMoviesDto } from './dto/query-movies.dto';
export declare class MoviesController {
    private readonly moviesService;
    constructor(moviesService: MoviesService);
    findAll(query: QueryMoviesDto): Promise<{
        movies: import("../../schemas/movie.schema").MovieDocument[];
        total: number;
        page: number;
        pages: number;
    }>;
    getTrending(limit?: number, contentType?: string): Promise<any[]>;
    getNewReleases(limit?: number): Promise<any[]>;
    getTopRated(limit?: number): Promise<any[]>;
    getPremium(limit?: number): Promise<any[]>;
    getByGenre(genre: string, limit?: number): Promise<any[]>;
    getByType(type: string, limit?: number): Promise<any[]>;
    findById(id: string): Promise<import("../../schemas/movie.schema").MovieDocument>;
    getRelated(id: string): Promise<any[]>;
    trackView(id: string, req: any): Promise<{
        tracked: boolean;
    }>;
    findByIdAdmin(id: string): Promise<import("../../schemas/movie.schema").MovieDocument>;
    create(dto: CreateMovieDto): Promise<import("../../schemas/movie.schema").MovieDocument>;
    update(id: string, dto: UpdateMovieDto): Promise<import("../../schemas/movie.schema").MovieDocument>;
    delete(id: string): Promise<{
        message: string;
    }>;
}
