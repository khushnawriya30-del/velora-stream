import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Banner, BannerDocument } from '../../schemas/banner.schema';
import { Movie, MovieDocument, ContentStatus } from '../../schemas/movie.schema';

@Injectable()
export class BannersService {
  constructor(
    @InjectModel(Banner.name) private bannerModel: Model<BannerDocument>,
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
  ) {}

  async getActiveBanners(): Promise<any[]> {
    try {
      const now = new Date();
      const banners = await this.bannerModel
        .find({
          isActive: true,
          $or: [
            { activeFrom: { $exists: false }, activeTo: { $exists: false } },
            { activeFrom: { $lte: now }, activeTo: { $gte: now } },
            { activeFrom: { $lte: now }, activeTo: { $exists: false } },
            { activeFrom: { $exists: false }, activeTo: { $gte: now } },
          ],
        })
        .sort({ displayOrder: 1 });
      
      // Filter out banners with invalid contentId before populating
      const validBanners = banners.filter((banner) => {
        if (!banner.contentId) return true;
        
        const contentIdStr = String(banner.contentId);
        if (contentIdStr.trim() === '') {
          return false;
        }
        
        try {
          new Types.ObjectId(contentIdStr);
          return true;
        } catch {
          return false;
        }
      });
      
      // Populate only valid banners — only include published movies
      const populatedBanners = await this.bannerModel.populate(validBanners, {
        path: 'contentId',
        select: 'title contentType genres contentRating duration releaseYear',
        match: { status: ContentStatus.PUBLISHED },
      });

      // Filter out banners where contentId became null (movie deleted or not published)
      const validPopulatedBanners = populatedBanners.filter(
        (b: any) => b.contentId != null,
      );

      if (validPopulatedBanners.length > 0) {
        return validPopulatedBanners;
      }

      // Fallback: auto-generate banners from recent published movies
      const recentMovies = await this.movieModel
        .find({ status: ContentStatus.PUBLISHED })
        .sort({ createdAt: -1 })
        .limit(5)
        .select('title synopsis bannerUrl posterUrl logoUrl genres contentType contentRating duration releaseYear');

      return recentMovies.map((movie, index) => ({
        _id: `auto_${movie._id}`,
        title: movie.title,
        subtitle: movie.synopsis ? movie.synopsis.substring(0, 100) : '',
        imageUrl: movie.bannerUrl || movie.posterUrl || '',
        contentId: movie.toObject(),
        actionType: 'movie',
        logoUrl: movie.logoUrl || '',
        genreTags: movie.genres || [],
        displayOrder: index,
        isActive: true,
      }));
    } catch (error) {
      console.error('Error fetching active banners:', error);
      return [];
    }
  }

  async getAll(): Promise<BannerDocument[]> {
    try {
      const banners = await this.bannerModel.find().sort({ displayOrder: 1 }).populate('contentId', 'title');
      console.log('Returning banners from getAll():', JSON.stringify(banners, null, 2));
      return banners;
    } catch (error) {
      // If populate fails due to invalid ObjectId, try without populate
      console.error('Error fetching all banners with populate:', error);
      const banners = await this.bannerModel.find().sort({ displayOrder: 1 });
      console.log('Returning banners without populate:', JSON.stringify(banners, null, 2));
      return banners;
    }
  }

  async create(data: Partial<Banner>): Promise<BannerDocument> {
    console.log('Creating banner with data:', JSON.stringify(data, null, 2));
    const banner = await this.bannerModel.create(data);
    console.log('Created banner:', JSON.stringify(banner, null, 2));
    return banner;
  }

  async update(id: string, data: Partial<Banner>): Promise<BannerDocument> {
    const banner = await this.bannerModel.findByIdAndUpdate(id, data, { new: true });
    if (!banner) throw new NotFoundException('Banner not found');
    return banner;
  }

  async delete(id: string): Promise<void> {
    const result = await this.bannerModel.findByIdAndDelete(id);
    if (!result) throw new NotFoundException('Banner not found');
  }

  async reorder(orderedIds: string[]): Promise<void> {
    const bulkOps = orderedIds.map((id, index) => ({
      updateOne: {
        filter: { _id: id },
        update: { displayOrder: index },
      },
    }));
    await this.bannerModel.bulkWrite(bulkOps);
  }
}
