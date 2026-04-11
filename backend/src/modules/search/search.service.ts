import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Movie, MovieDocument, ContentStatus } from '../../schemas/movie.schema';
import { SearchQuery, SearchQueryDocument } from '../../schemas/search-query.schema';

@Injectable()
export class SearchService {
  constructor(
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
    @InjectModel(SearchQuery.name) private searchQueryModel: Model<SearchQueryDocument>,
  ) {}

  async search(
    query: string,
    filters?: {
      contentType?: string;
      genre?: string;
      language?: string;
      yearMin?: number;
      yearMax?: number;
      ratingMin?: number;
      sort?: string;
      platform?: string;
    },
    page = 1,
    limit = 20,
  ): Promise<{ results: MovieDocument[]; total: number }> {
    const skip = (page - 1) * limit;
    const filter: any = { status: ContentStatus.PUBLISHED };

    if (query && query.trim()) {
      filter.$text = { $search: query };
    }

    if (filters?.contentType) filter.contentType = filters.contentType;
    if (filters?.genre) filter.genres = filters.genre;
    if (filters?.language) filter.languages = filters.language;
    if (filters?.yearMin || filters?.yearMax) {
      filter.releaseYear = {};
      if (filters.yearMin) filter.releaseYear.$gte = filters.yearMin;
      if (filters.yearMax) filter.releaseYear.$lte = filters.yearMax;
    }
    if (filters?.ratingMin) filter.rating = { $gte: filters.ratingMin };
    if (filters?.platform) filter.platformOrigin = { $regex: filters.platform, $options: 'i' };

    let sortObj: any = {};
    if (query && query.trim() && !filters?.sort) {
      sortObj = { score: { $meta: 'textScore' }, popularityScore: -1 };
    } else if (filters?.sort === 'rating') {
      sortObj = { rating: -1 };
    } else if (filters?.sort === 'newest') {
      sortObj = { releaseYear: -1, createdAt: -1 };
    } else if (filters?.sort === 'views') {
      sortObj = { viewCount: -1 };
    } else if (filters?.sort === 'title') {
      sortObj = { title: 1 };
    } else {
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
        .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating languages'),
      this.movieModel.countDocuments(filter),
    ]);

    // Track search query for popular searches
    if (query && query.trim().length >= 2) {
      this.trackSearchQuery(query.trim()).catch(() => {});
    }

    return { results, total };
  }

  private async trackSearchQuery(query: string): Promise<void> {
    const normalized = query.toLowerCase().trim();
    // Find the best matching movie for this query
    const matchingMovie = await this.movieModel.findOne({
      status: ContentStatus.PUBLISHED,
      title: { $regex: normalized.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), $options: 'i' },
    }).select('_id posterUrl contentType').lean();

    await this.searchQueryModel.findOneAndUpdate(
      { query: normalized },
      {
        $inc: { count: 1 },
        ...(matchingMovie ? {
          $set: {
            contentId: matchingMovie._id.toString(),
            posterUrl: matchingMovie.posterUrl,
            contentType: matchingMovie.contentType,
          },
        } : {}),
      },
      { upsert: true },
    );
  }

  async autocomplete(query: string): Promise<any[]> {
    if (!query || query.trim().length < 2) return [];

    // Normalize: strip special chars for matching
    const normalized = query.replace(/[:_\-\/]/g, ' ').replace(/\s+/g, ' ').trim();
    const words = normalized.split(' ').filter(w => w.length > 0);
    // Build regex that matches all words in any order
    const regexParts = words.map(w => `(?=.*${w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`);
    const regexStr = regexParts.join('') + '.*';

    return this.movieModel
      .find({
        status: ContentStatus.PUBLISHED,
        title: { $regex: regexStr, $options: 'i' },
      })
      .sort({ popularityScore: -1 })
      .limit(10)
      .select('title posterUrl contentType releaseYear');
  }

  async getTrendingSearches(): Promise<string[]> {
    // Return top searches from tracked query data
    const tracked = await this.searchQueryModel
      .find({ count: { $gte: 1 } })
      .sort({ count: -1 })
      .limit(10)
      .lean();

    if (tracked.length >= 3) {
      return tracked.map(t => t.query);
    }

    // Fallback: use most viewed content titles
    const trending = await this.movieModel
      .find({ status: ContentStatus.PUBLISHED })
      .sort({ viewCount: -1 })
      .limit(10)
      .select('title');
    return trending.map((m) => m.title);
  }

  async getMostPopularSearches(): Promise<any[]> {
    // Get top 3 most searched with poster info
    const tracked = await this.searchQueryModel
      .find({ count: { $gte: 1 }, contentId: { $ne: null } })
      .sort({ count: -1 })
      .limit(3)
      .lean();

    if (tracked.length >= 1) {
      // Resolve movie data for each
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
      if (results.length > 0) return results;
    }

    // Fallback: most viewed
    return this.movieModel
      .find({ status: ContentStatus.PUBLISHED })
      .sort({ viewCount: -1 })
      .limit(3)
      .select('title posterUrl bannerUrl contentType releaseYear rating');
  }

  async getRecommended(limit = 10): Promise<any[]> {
    return this.movieModel
      .find({ status: ContentStatus.PUBLISHED })
      .sort({ popularityScore: -1, viewCount: -1 })
      .limit(limit)
      .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating languages');
  }

  async getGenres(): Promise<string[]> {
    return this.movieModel.distinct('genres', { status: ContentStatus.PUBLISHED });
  }

  async getLanguages(): Promise<string[]> {
    return this.movieModel.distinct('languages', { status: ContentStatus.PUBLISHED });
  }

  async getPlatforms(): Promise<string[]> {
    return this.movieModel.distinct('platformOrigin', {
      status: ContentStatus.PUBLISHED,
      platformOrigin: { $nin: [null, ''] },
    });
  }

  async getYears(): Promise<number[]> {
    const years = await this.movieModel.distinct('releaseYear', {
      status: ContentStatus.PUBLISHED,
      releaseYear: { $ne: null },
    });
    return years.sort((a, b) => b - a);
  }

  async getRanking(
    type = 'download',
    contentType?: string,
    genre?: string,
    limit = 20,
  ): Promise<MovieDocument[]> {
    const filter: any = { status: ContentStatus.PUBLISHED };
    if (contentType) filter.contentType = contentType;
    if (genre) filter.genres = genre;

    let sortObj: any;
    if (type === 'rating') {
      sortObj = { starRating: -1, rating: -1, voteCount: -1 };
    } else {
      // download rank = popularity + views
      sortObj = { popularityScore: -1, viewCount: -1 };
    }

    return this.movieModel
      .find(filter)
      .sort(sortObj)
      .limit(limit)
      .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating starRating languages viewCount popularityScore videoQuality country');
  }
}
