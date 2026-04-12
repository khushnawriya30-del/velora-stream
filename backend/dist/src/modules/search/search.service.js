"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SearchService = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const mongoose_2 = require("mongoose");
const movie_schema_1 = require("../../schemas/movie.schema");
const search_query_schema_1 = require("../../schemas/search-query.schema");
const premium_enrichment_1 = require("../../utils/premium-enrichment");
let SearchService = class SearchService {
    constructor(movieModel, searchQueryModel) {
        this.movieModel = movieModel;
        this.searchQueryModel = searchQueryModel;
    }
    async search(query, filters, page = 1, limit = 20) {
        const skip = (page - 1) * limit;
        const filter = { status: movie_schema_1.ContentStatus.PUBLISHED };
        if (query && query.trim()) {
            filter.$text = { $search: query };
        }
        if (filters?.contentType)
            filter.contentType = filters.contentType;
        if (filters?.genre)
            filter.genres = filters.genre;
        if (filters?.language)
            filter.languages = filters.language;
        if (filters?.yearMin || filters?.yearMax) {
            filter.releaseYear = {};
            if (filters.yearMin)
                filter.releaseYear.$gte = filters.yearMin;
            if (filters.yearMax)
                filter.releaseYear.$lte = filters.yearMax;
        }
        if (filters?.ratingMin)
            filter.rating = { $gte: filters.ratingMin };
        if (filters?.platform)
            filter.ottPlatforms = { $regex: filters.platform, $options: 'i' };
        let sortObj = {};
        if (query && query.trim() && !filters?.sort) {
            sortObj = { score: { $meta: 'textScore' }, popularityScore: -1 };
        }
        else if (filters?.sort === 'rating') {
            sortObj = { rating: -1 };
        }
        else if (filters?.sort === 'newest') {
            sortObj = { releaseYear: -1, createdAt: -1 };
        }
        else if (filters?.sort === 'views') {
            sortObj = { viewCount: -1 };
        }
        else if (filters?.sort === 'title') {
            sortObj = { title: 1 };
        }
        else {
            sortObj = { popularityScore: -1 };
        }
        const projection = query && query.trim()
            ? { score: { $meta: 'textScore' } }
            : {};
        const [results, total] = await Promise.all([
            this.movieModel
                .find(filter, projection)
                .sort(sortObj)
                .skip(skip)
                .limit(limit)
                .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating languages isPremium'),
            this.movieModel.countDocuments(filter),
        ]);
        if (query && query.trim().length >= 2) {
            this.trackSearchQuery(query.trim()).catch(() => { });
        }
        const enriched = await (0, premium_enrichment_1.enrichWithPremiumEpisodeFlag)(this.movieModel, results);
        return { results: enriched, total };
    }
    async trackSearchQuery(query) {
        const normalized = query.toLowerCase().trim();
        const matchingMovie = await this.movieModel.findOne({
            status: movie_schema_1.ContentStatus.PUBLISHED,
            title: { $regex: normalized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), $options: 'i' },
        }).select('_id posterUrl contentType').lean();
        await this.searchQueryModel.findOneAndUpdate({ query: normalized }, {
            $inc: { count: 1 },
            ...(matchingMovie ? {
                $set: {
                    contentId: matchingMovie._id.toString(),
                    posterUrl: matchingMovie.posterUrl,
                    contentType: matchingMovie.contentType,
                },
            } : {}),
        }, { upsert: true });
    }
    async autocomplete(query) {
        if (!query || query.trim().length < 2)
            return [];
        const normalized = query.replace(/[:_\-\/]/g, ' ').replace(/\s+/g, ' ').trim();
        const words = normalized.split(' ').filter(w => w.length > 0);
        const regexParts = words.map(w => `(?=.*${w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`);
        const regexStr = regexParts.join('') + '.*';
        return this.movieModel
            .find({
            status: movie_schema_1.ContentStatus.PUBLISHED,
            title: { $regex: regexStr, $options: 'i' },
        })
            .sort({ popularityScore: -1 })
            .limit(10)
            .select('title posterUrl contentType releaseYear');
    }
    async getTrendingSearches() {
        const tracked = await this.searchQueryModel
            .find({ count: { $gte: 1 } })
            .sort({ count: -1 })
            .limit(10)
            .lean();
        if (tracked.length >= 3) {
            return tracked.map(t => t.query);
        }
        const trending = await this.movieModel
            .find({ status: movie_schema_1.ContentStatus.PUBLISHED })
            .sort({ viewCount: -1 })
            .limit(10)
            .select('title');
        return trending.map((m) => m.title);
    }
    async getMostPopularSearches() {
        const tracked = await this.searchQueryModel
            .find({ count: { $gte: 1 }, contentId: { $ne: null } })
            .sort({ count: -1 })
            .limit(3)
            .lean();
        if (tracked.length >= 1) {
            const results = [];
            for (const t of tracked) {
                if (t.contentId) {
                    const movie = await this.movieModel
                        .findById(t.contentId)
                        .select('title posterUrl bannerUrl contentType releaseYear rating')
                        .lean();
                    if (movie) {
                        results.push({ ...movie, searchCount: t.count });
                    }
                }
            }
            if (results.length > 0)
                return results;
        }
        return this.movieModel
            .find({ status: movie_schema_1.ContentStatus.PUBLISHED })
            .sort({ viewCount: -1 })
            .limit(3)
            .select('title posterUrl bannerUrl contentType releaseYear rating');
    }
    async getRecommended(limit = 10) {
        const movies = await this.movieModel
            .find({ status: movie_schema_1.ContentStatus.PUBLISHED })
            .sort({ popularityScore: -1, viewCount: -1 })
            .limit(limit)
            .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating languages isPremium');
        return (0, premium_enrichment_1.enrichWithPremiumEpisodeFlag)(this.movieModel, movies);
    }
    async getGenres() {
        return this.movieModel.distinct('genres', { status: movie_schema_1.ContentStatus.PUBLISHED });
    }
    async getLanguages() {
        return this.movieModel.distinct('languages', { status: movie_schema_1.ContentStatus.PUBLISHED });
    }
    async getPlatforms() {
        return this.movieModel.distinct('ottPlatforms', {
            status: movie_schema_1.ContentStatus.PUBLISHED,
            ottPlatforms: { $exists: true, $ne: [] },
        });
    }
    async getYears() {
        const years = await this.movieModel.distinct('releaseYear', {
            status: movie_schema_1.ContentStatus.PUBLISHED,
            releaseYear: { $ne: null },
        });
        return years.sort((a, b) => b - a);
    }
    async getRanking(type = 'download', contentType, genre, limit = 20) {
        const filter = { status: movie_schema_1.ContentStatus.PUBLISHED };
        if (contentType)
            filter.contentType = contentType;
        if (genre)
            filter.genres = genre;
        let sortObj;
        if (type === 'rating') {
            sortObj = { starRating: -1, rating: -1, voteCount: -1 };
        }
        else {
            sortObj = { popularityScore: -1, viewCount: -1 };
        }
        return this.movieModel
            .find(filter)
            .sort(sortObj)
            .limit(limit)
            .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating starRating languages viewCount popularityScore videoQuality country');
    }
};
exports.SearchService = SearchService;
exports.SearchService = SearchService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, mongoose_1.InjectModel)(movie_schema_1.Movie.name)),
    __param(1, (0, mongoose_1.InjectModel)(search_query_schema_1.SearchQuery.name)),
    __metadata("design:paramtypes", [mongoose_2.Model,
        mongoose_2.Model])
], SearchService);
//# sourceMappingURL=search.service.js.map