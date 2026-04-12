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
exports.TmdbService = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const config_1 = require("@nestjs/config");
const mongoose_2 = require("mongoose");
const settings_service_1 = require("../settings/settings.service");
const movie_schema_1 = require("../../schemas/movie.schema");
const TMDB_BASE = 'https://api.themoviedb.org/3';
const TMDB_IMG = 'https://image.tmdb.org/t/p';
const REGION_MAP = {
    bollywood: { language: 'hi-IN', region: 'IN', with_original_language: 'hi' },
    hollywood: { language: 'en-US', region: 'US', with_original_language: 'en' },
    korean: { language: 'ko-KR', region: 'KR', with_original_language: 'ko' },
    japanese: { language: 'ja-JP', region: 'JP', with_original_language: 'ja' },
    chinese: { language: 'zh-CN', region: 'CN', with_original_language: 'zh' },
    tamil: { language: 'ta-IN', region: 'IN', with_original_language: 'ta' },
    telugu: { language: 'te-IN', region: 'IN', with_original_language: 'te' },
    malayalam: { language: 'ml-IN', region: 'IN', with_original_language: 'ml' },
    kannada: { language: 'kn-IN', region: 'IN', with_original_language: 'kn' },
    thai: { language: 'th-TH', region: 'TH', with_original_language: 'th' },
    spanish: { language: 'es-ES', region: 'ES', with_original_language: 'es' },
    french: { language: 'fr-FR', region: 'FR', with_original_language: 'fr' },
    turkish: { language: 'tr-TR', region: 'TR', with_original_language: 'tr' },
};
const GENRE_MAP = {
    28: 'Action', 12: 'Adventure', 16: 'Animation', 35: 'Comedy', 80: 'Crime',
    99: 'Documentary', 18: 'Drama', 10751: 'Family', 14: 'Fantasy', 36: 'History',
    27: 'Horror', 10402: 'Music', 9648: 'Mystery', 10749: 'Romance', 878: 'Sci-Fi',
    10770: 'TV Movie', 53: 'Thriller', 10752: 'War', 37: 'Western',
    10759: 'Action & Adventure', 10762: 'Kids', 10763: 'News', 10764: 'Reality',
    10765: 'Sci-Fi & Fantasy', 10766: 'Soap', 10767: 'Talk', 10768: 'War & Politics',
};
const TMDB_PROVIDER_MAP = {
    'netflix': 'Netflix',
    'netflix basic with ads': 'Netflix',
    'amazon prime video': 'Amazon Prime Video',
    'amazon video': 'Amazon Prime Video',
    'disney plus': 'Disney+ Hotstar',
    'disney+ hotstar': 'Disney+ Hotstar',
    'hotstar': 'Disney+ Hotstar',
    'jiocinema': 'JioCinema',
    'jio cinema': 'JioCinema',
    'sonyliv': 'SonyLIV',
    'zee5': 'Zee5',
    'apple tv plus': 'Apple TV+',
    'apple tv+': 'Apple TV+',
    'apple itunes': 'Apple TV+',
    'hulu': 'Hulu',
    'hbo max': 'HBO Max',
    'max': 'HBO Max',
    'max amazon channel': 'HBO Max',
    'paramount plus': 'Paramount+',
    'paramount+ amazon channel': 'Paramount+',
    'paramount plus apple tv channel': 'Paramount+',
    'peacock': 'Peacock',
    'peacock premium': 'Peacock',
    'mx player': 'MX Player',
    'voot': 'Voot',
    'jio voot': 'Voot',
    'altbalaji': 'ALTBalaji',
    'aha': 'Aha',
    'hoichoi': 'Hoichoi',
    'lionsgate play': 'Lionsgate Play',
    'crunchyroll': 'Crunchyroll',
};
let TmdbService = class TmdbService {
    constructor(movieModel, configService, settingsService) {
        this.movieModel = movieModel;
        this.configService = configService;
        this.settingsService = settingsService;
        this.envToken = this.configService.get('TMDB_ACCESS_TOKEN', '');
    }
    async getToken() {
        if (this.envToken)
            return this.envToken;
        return this.settingsService.getTmdbToken();
    }
    async tmdbGet(path, params = {}) {
        const token = await this.getToken();
        if (!token)
            throw new common_1.BadRequestException('TMDB access token not configured');
        const url = new URL(`${TMDB_BASE}${path}`);
        Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
        const res = await fetch(url.toString(), {
            headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
        });
        if (!res.ok) {
            const body = await res.text();
            throw new common_1.BadRequestException(`TMDB API error (${res.status}): ${body}`);
        }
        return res.json();
    }
    async discover(opts) {
        const { contentType, region, count, year, genres, withLanguage, withCast, releaseStatus, withWatchProviders, watchRegion } = opts;
        const regionCfg = REGION_MAP[region.toLowerCase()] ?? { language: 'en-US' };
        const isAnime = contentType === 'anime';
        const mediaType = (contentType === 'movies') ? 'movie' : 'tv';
        const startPage = opts.page || 1;
        const pagesNeeded = Math.ceil(count / 20);
        const allResults = [];
        for (let p = startPage; p < startPage + pagesNeeded && allResults.length < count; p++) {
            const params = {
                language: withLanguage ? `${withLanguage}-${regionCfg.region ?? 'US'}` : regionCfg.language,
                sort_by: 'popularity.desc',
                page: String(p),
                'vote_count.gte': '5',
            };
            if (!withLanguage && regionCfg.with_original_language) {
                params.with_original_language = regionCfg.with_original_language;
            }
            if (regionCfg.region && mediaType === 'movie') {
                params.region = regionCfg.region;
            }
            if (isAnime) {
                params.with_genres = '16';
                if (!withLanguage)
                    params.with_original_language = 'ja';
            }
            if (year) {
                if (mediaType === 'movie') {
                    params.primary_release_year = String(year);
                }
                else {
                    params.first_air_date_year = String(year);
                }
            }
            if (genres && genres.length > 0) {
                const existing = params.with_genres ? [params.with_genres] : [];
                params.with_genres = [...existing, ...genres.map(String)].join(',');
            }
            if (withCast) {
                params.with_cast = withCast;
            }
            if (releaseStatus === 'upcoming') {
                const today = new Date().toISOString().slice(0, 10);
                if (mediaType === 'movie') {
                    params['primary_release_date.gte'] = today;
                }
                else {
                    params['first_air_date.gte'] = today;
                }
                params.sort_by = 'primary_release_date.asc';
                delete params['vote_count.gte'];
            }
            else if (releaseStatus === 'released') {
                const today = new Date().toISOString().slice(0, 10);
                if (mediaType === 'movie') {
                    params['primary_release_date.lte'] = today;
                }
                else {
                    params['first_air_date.lte'] = today;
                }
            }
            if (withWatchProviders) {
                params.with_watch_providers = withWatchProviders;
                params.watch_region = watchRegion ?? 'IN';
            }
            const data = await this.tmdbGet(`/discover/${mediaType}`, params);
            allResults.push(...(data.results ?? []));
        }
        const results = allResults.slice(0, count);
        const nextPage = startPage + pagesNeeded;
        const tmdbIds = results.map((r) => String(r.id));
        const existing = await this.movieModel.find({ tmdbId: { $in: tmdbIds } }).select('tmdbId');
        const existingSet = new Set(existing.map((m) => m.tmdbId));
        return {
            items: results.map((r) => ({
                tmdbId: r.id,
                title: r.title ?? r.name ?? '',
                overview: r.overview ?? '',
                posterUrl: r.poster_path ? `${TMDB_IMG}/w500${r.poster_path}` : null,
                backdropUrl: r.backdrop_path ? `${TMDB_IMG}/w1280${r.backdrop_path}` : null,
                releaseDate: r.release_date ?? r.first_air_date ?? '',
                rating: r.vote_average ?? 0,
                genreNames: (r.genre_ids ?? []).map((id) => GENRE_MAP[id] ?? 'Other').filter(Boolean),
                originalLanguage: r.original_language ?? '',
                alreadyImported: existingSet.has(String(r.id)),
            })),
            nextPage,
        };
    }
    async searchPerson(query) {
        if (!query?.trim())
            throw new common_1.BadRequestException('Search query is required');
        const data = await this.tmdbGet('/search/person', {
            query: query.trim(),
            page: '1',
            language: 'en-US',
            include_adult: 'false',
        });
        const results = (data.results ?? []).slice(0, 10).map((p) => ({
            id: p.id,
            name: p.name,
            profileUrl: p.profile_path ? `${TMDB_IMG}/w185${p.profile_path}` : null,
            knownFor: (p.known_for ?? []).map((k) => k.title ?? k.name ?? '').filter(Boolean).slice(0, 3).join(', '),
        }));
        return { results };
    }
    async search(opts) {
        const { query, contentType, watchProviders, watchRegion, withOriginalLanguage } = opts;
        const page = opts.page || 1;
        if (!query?.trim())
            throw new common_1.BadRequestException('Search query is required');
        const mediaType = contentType === 'movies' ? 'movie' : 'tv';
        const params = {
            query: query.trim(),
            page: String(page),
            language: 'en-US',
            include_adult: 'false',
        };
        const data = await this.tmdbGet(`/search/${mediaType}`, params);
        let results = data.results ?? [];
        if (contentType === 'anime') {
            results = results.filter((r) => (r.genre_ids ?? []).includes(16));
        }
        if (withOriginalLanguage) {
            results = results.filter((r) => r.original_language === withOriginalLanguage);
        }
        if (watchProviders && watchRegion) {
            results = await this.filterByWatchProviders(results, mediaType, watchProviders, watchRegion);
        }
        const tmdbIds = results.map((r) => String(r.id));
        const existing = await this.movieModel.find({ tmdbId: { $in: tmdbIds } }).select('tmdbId');
        const existingSet = new Set(existing.map((m) => m.tmdbId));
        return {
            items: results.map((r) => ({
                tmdbId: r.id,
                title: r.title ?? r.name ?? '',
                overview: r.overview ?? '',
                posterUrl: r.poster_path ? `${TMDB_IMG}/w500${r.poster_path}` : null,
                backdropUrl: r.backdrop_path ? `${TMDB_IMG}/w1280${r.backdrop_path}` : null,
                releaseDate: r.release_date ?? r.first_air_date ?? '',
                rating: r.vote_average ?? 0,
                genreNames: (r.genre_ids ?? []).map((id) => GENRE_MAP[id] ?? 'Other').filter(Boolean),
                originalLanguage: r.original_language ?? '',
                alreadyImported: existingSet.has(String(r.id)),
            })),
            nextPage: page + 1,
            totalResults: data.total_results ?? 0,
        };
    }
    async importItems(tmdbIds, contentType, asUpcoming = false) {
        const mediaType = (contentType === 'movies') ? 'movie' : 'tv';
        let imported = 0;
        let skipped = 0;
        const items = [];
        for (const tmdbId of tmdbIds) {
            const exists = await this.movieModel.findOne({ tmdbId: String(tmdbId) });
            if (exists) {
                skipped++;
                items.push({ tmdbId, title: exists.title, status: 'skipped', reason: 'already exists' });
                continue;
            }
            try {
                const movie = await this.fetchAndCreateMovie(tmdbId, mediaType, contentType, asUpcoming);
                imported++;
                items.push({ tmdbId, title: movie.title, status: 'imported', id: movie._id });
            }
            catch (err) {
                skipped++;
                items.push({ tmdbId, status: 'error', reason: err.message });
            }
        }
        return { imported, skipped, items };
    }
    async filterByWatchProviders(results, mediaType, providerId, region) {
        const checks = await Promise.all(results.map(async (item) => {
            try {
                const data = await this.tmdbGet(`/${mediaType}/${item.id}/watch/providers`);
                const regionData = data.results?.[region];
                if (!regionData)
                    return { item, hasProvider: false };
                const allProviders = [
                    ...(regionData.flatrate ?? []),
                    ...(regionData.buy ?? []),
                    ...(regionData.rent ?? []),
                    ...(regionData.ads ?? []),
                    ...(regionData.free ?? []),
                ];
                const hasProvider = allProviders.some((p) => String(p.provider_id) === providerId);
                return { item, hasProvider };
            }
            catch {
                return { item, hasProvider: false };
            }
        }));
        return checks.filter((c) => c.hasProvider).map((c) => c.item);
    }
    async fetchOttPlatforms(tmdbId, mediaType) {
        try {
            const data = await this.tmdbGet(`/${mediaType}/${tmdbId}/watch/providers`);
            const regions = ['IN', 'US', 'GB'];
            const providerNames = new Set();
            for (const region of regions) {
                const regionData = data.results?.[region];
                if (!regionData)
                    continue;
                const providers = [
                    ...(regionData.flatrate ?? []),
                    ...(regionData.ads ?? []),
                    ...(regionData.free ?? []),
                ];
                for (const p of providers) {
                    const name = (p.provider_name ?? '').toLowerCase().trim();
                    const mapped = TMDB_PROVIDER_MAP[name];
                    if (mapped)
                        providerNames.add(mapped);
                }
            }
            return Array.from(providerNames);
        }
        catch {
            return [];
        }
    }
    async fetchAndCreateMovie(tmdbId, mediaType, contentType, asUpcoming = false) {
        const [detail, ottPlatforms] = await Promise.all([
            this.tmdbGet(`/${mediaType}/${tmdbId}`, {
                language: 'en-US',
                append_to_response: 'credits',
            }),
            this.fetchOttPlatforms(tmdbId, mediaType),
        ]);
        let appContentType;
        if (contentType === 'anime') {
            appContentType = movie_schema_1.ContentType.ANIME;
        }
        else if (contentType === 'webseries') {
            appContentType = movie_schema_1.ContentType.WEB_SERIES;
        }
        else if (contentType === 'shows') {
            appContentType = detail.number_of_seasons ? movie_schema_1.ContentType.WEB_SERIES : movie_schema_1.ContentType.TV_SHOW;
        }
        else {
            appContentType = movie_schema_1.ContentType.MOVIE;
        }
        const genres = (detail.genres ?? []).map((g) => g.name);
        const languages = [];
        if (detail.original_language) {
            const langMap = {
                en: 'English', hi: 'Hindi', ko: 'Korean', ja: 'Japanese', zh: 'Chinese',
                ta: 'Tamil', te: 'Telugu', ml: 'Malayalam', kn: 'Kannada', th: 'Thai',
                es: 'Spanish', fr: 'French', tr: 'Turkish', de: 'German', pt: 'Portuguese',
                it: 'Italian', ru: 'Russian', ar: 'Arabic',
            };
            languages.push(langMap[detail.original_language] ?? detail.original_language);
        }
        if (detail.spoken_languages) {
            for (const sl of detail.spoken_languages) {
                const name = sl.english_name ?? sl.name;
                if (name && !languages.includes(name))
                    languages.push(name);
            }
        }
        const credits = detail.credits ?? {};
        const cast = (credits.cast ?? []).slice(0, 15).map((c) => ({
            name: c.name,
            character: c.character ?? '',
            role: 'Actor',
            photoUrl: c.profile_path ? `${TMDB_IMG}/w185${c.profile_path}` : '',
        }));
        const crew = credits.crew ?? [];
        const directors = crew.filter((c) => c.job === 'Director').map((c) => c.name);
        const writers = crew.filter((c) => ['Writer', 'Screenplay', 'Story'].includes(c.job)).map((c) => c.name);
        const producers = crew.filter((c) => c.job === 'Producer').map((c) => c.name);
        if (directors.length) {
            cast.push(...directors.map((name) => ({
                name, character: '', role: 'Director', photoUrl: '',
            })));
        }
        if (writers.length) {
            cast.push(...writers.slice(0, 3).map((name) => ({
                name, character: '', role: 'Writer', photoUrl: '',
            })));
        }
        if (producers.length) {
            cast.push(...producers.slice(0, 3).map((name) => ({
                name, character: '', role: 'Producer', photoUrl: '',
            })));
        }
        const dateStr = detail.release_date ?? detail.first_air_date ?? '';
        const releaseYear = dateStr ? parseInt(dateStr.split('-')[0], 10) : undefined;
        const duration = mediaType === 'movie'
            ? detail.runtime ?? undefined
            : detail.episode_run_time?.[0] ?? undefined;
        const movieData = {
            title: detail.title ?? detail.name ?? '',
            alternateTitle: detail.original_title ?? detail.original_name ?? '',
            synopsis: detail.overview ?? '',
            contentType: appContentType,
            genres,
            languages,
            status: asUpcoming ? movie_schema_1.ContentStatus.UPCOMING : movie_schema_1.ContentStatus.PUBLISHED,
            releaseYear,
            releaseDate: dateStr ? new Date(dateStr) : undefined,
            country: (detail.production_countries ?? []).map((c) => c.name).join(', '),
            duration,
            director: directors.join(', '),
            studio: (detail.production_companies ?? []).map((c) => c.name).join(', '),
            cast,
            posterUrl: detail.poster_path ? `${TMDB_IMG}/w500${detail.poster_path}` : '',
            bannerUrl: detail.backdrop_path ? `${TMDB_IMG}/w1280${detail.backdrop_path}` : '',
            tmdbId: String(tmdbId),
            imdbId: detail.imdb_id ?? '',
            rating: detail.vote_average ?? 0,
            starRating: detail.vote_average ? Math.round(detail.vote_average * 10) / 10 : 0,
            voteCount: detail.vote_count ?? 0,
            popularityScore: Math.round(detail.popularity ?? 0),
            tags: (detail.keywords?.keywords ?? detail.keywords?.results ?? []).map((k) => k.name).slice(0, 10),
            platformOrigin: 'tmdb',
            ottPlatforms,
        };
        return this.movieModel.create(movieData);
    }
};
exports.TmdbService = TmdbService;
exports.TmdbService = TmdbService = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, mongoose_1.InjectModel)(movie_schema_1.Movie.name)),
    __metadata("design:paramtypes", [mongoose_2.Model,
        config_1.ConfigService,
        settings_service_1.SettingsService])
], TmdbService);
//# sourceMappingURL=tmdb.service.js.map