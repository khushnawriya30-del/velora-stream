import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Watchlist, WatchlistDocument } from '../../schemas/watchlist.schema';

@Injectable()
export class WatchlistService {
  constructor(@InjectModel(Watchlist.name) private watchlistModel: Model<WatchlistDocument>) {}

  async addToWatchlist(userId: string, profileId: string, contentId: string): Promise<WatchlistDocument> {
    return this.watchlistModel.findOneAndUpdate(
      {
        userId: new Types.ObjectId(userId),
        profileId: new Types.ObjectId(profileId),
        contentId: new Types.ObjectId(contentId),
      },
      {},
      { upsert: true, new: true },
    );
  }

  async removeFromWatchlist(userId: string, profileId: string, contentId: string): Promise<void> {
    await this.watchlistModel.deleteOne({
      userId: new Types.ObjectId(userId),
      profileId: new Types.ObjectId(profileId),
      contentId: new Types.ObjectId(contentId),
    });
  }

  async getWatchlist(userId: string, profileId: string): Promise<WatchlistDocument[]> {
    return this.watchlistModel
      .find({
        userId: new Types.ObjectId(userId),
        profileId: new Types.ObjectId(profileId),
      })
      .sort({ createdAt: -1 })
      .populate('contentId', 'title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating');
  }

  async isInWatchlist(userId: string, profileId: string, contentId: string): Promise<boolean> {
    const item = await this.watchlistModel.findOne({
      userId: new Types.ObjectId(userId),
      profileId: new Types.ObjectId(profileId),
      contentId: new Types.ObjectId(contentId),
    });
    return !!item;
  }
}
