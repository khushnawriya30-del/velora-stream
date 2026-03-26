import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Banner, BannerDocument } from '../../schemas/banner.schema';

@Injectable()
export class BannersService {
  constructor(@InjectModel(Banner.name) private bannerModel: Model<BannerDocument>) {}

  async getActiveBanners(): Promise<BannerDocument[]> {
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
        if (!banner.contentId) return true; // null or undefined is okay
        
        const contentIdStr = String(banner.contentId);
        if (contentIdStr.trim() === '') {
          return false; // Empty string
        }
        
        try {
          // Check if contentId is a valid ObjectId
          new Types.ObjectId(contentIdStr);
          return true;
        } catch {
          return false; // Invalid ObjectId
        }
      });
      
      // Populate only valid banners
      return this.bannerModel.populate(validBanners, {
        path: 'contentId',
        select: 'title contentType genres contentRating duration releaseYear',
      });
    } catch (error) {
      // If there's an error (e.g., invalid ObjectId in database), return empty array
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
