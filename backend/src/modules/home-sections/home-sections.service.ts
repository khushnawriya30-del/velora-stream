import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { HomeSection, HomeSectionDocument } from '../../schemas/home-section.schema';
import { Movie, MovieDocument, ContentStatus } from '../../schemas/movie.schema';

@Injectable()
export class HomeSectionsService {
  constructor(
    @InjectModel(HomeSection.name) private sectionModel: Model<HomeSectionDocument>,
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
  ) {}

  async getHomeFeed(): Promise<any[]> {
    const sections = await this.sectionModel
      .find({ isVisible: true })
      .sort({ displayOrder: 1 });

    const feed = [];
    for (const section of sections) {
      let movies: MovieDocument[];

      if (section.isSystemManaged) {
        const filter: any = { status: ContentStatus.PUBLISHED };
        if (section.contentType) filter.contentType = section.contentType;
        if (section.genre) filter.genres = section.genre;

        let sort: any = { createdAt: -1 };
        if (section.sortBy === 'popularityScore') sort = { popularityScore: -1, viewCount: -1 };
        if (section.sortBy === 'rating') sort = { rating: -1 };
        if (section.sortBy === 'viewCount') sort = { viewCount: -1 };

        movies = await this.movieModel
          .find(filter)
          .sort(sort)
          .limit(section.maxItems)
          .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating viewCount');
      } else {
        movies = await this.movieModel
          .find({ _id: { $in: section.contentIds }, status: ContentStatus.PUBLISHED })
          .select('title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating viewCount');
      }

      // Always include the section, even without movies (show section title at minimum)
      feed.push({
        id: section._id,
        title: section.title,
        type: section.type,
        slug: section.slug,
        cardSize: section.cardSize,
        showViewMore: section.showViewMore,
        viewMoreText: section.viewMoreText,
        showTrendingNumbers: section.showTrendingNumbers,
        bannerImageUrl: section.bannerImageUrl,
        items: movies,
      });
    }

    return feed;
  }

  async getAll(): Promise<HomeSectionDocument[]> {
    return this.sectionModel.find().sort({ displayOrder: 1 });
  }

  async create(data: Partial<HomeSection>): Promise<HomeSectionDocument> {
    const count = await this.sectionModel.countDocuments();
    return this.sectionModel.create({ ...data, displayOrder: count });
  }

  async update(id: string, data: Partial<HomeSection>): Promise<HomeSectionDocument> {
    const section = await this.sectionModel.findByIdAndUpdate(id, data, { new: true });
    if (!section) throw new NotFoundException('Section not found');
    return section;
  }

  async delete(id: string): Promise<void> {
    const result = await this.sectionModel.findByIdAndDelete(id);
    if (!result) throw new NotFoundException('Section not found');
  }

  async reorder(orderedIds: string[]): Promise<void> {
    const bulkOps = orderedIds.map((id, index) => ({
      updateOne: {
        filter: { _id: id },
        update: { displayOrder: index },
      },
    }));
    await this.sectionModel.bulkWrite(bulkOps);
  }
}
