import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { ThematicCollection, ThematicCollectionDocument } from '../../schemas/thematic-collection.schema';

@Injectable()
export class ThematicCollectionService {
  constructor(
    @InjectModel(ThematicCollection.name) private collectionModel: Model<ThematicCollectionDocument>,
  ) {}

  async addToCollection(userId: string, profileId: string, contentId: string): Promise<ThematicCollectionDocument> {
    return this.collectionModel.findOneAndUpdate(
      {
        userId: new Types.ObjectId(userId),
        profileId: new Types.ObjectId(profileId),
        contentId: new Types.ObjectId(contentId),
      },
      {},
      { upsert: true, new: true },
    );
  }

  async removeFromCollection(userId: string, profileId: string, contentId: string): Promise<void> {
    await this.collectionModel.deleteOne({
      userId: new Types.ObjectId(userId),
      profileId: new Types.ObjectId(profileId),
      contentId: new Types.ObjectId(contentId),
    });
  }

  async getCollection(userId: string, profileId: string): Promise<ThematicCollectionDocument[]> {
    return this.collectionModel
      .find({
        userId: new Types.ObjectId(userId),
        profileId: new Types.ObjectId(profileId),
      })
      .sort({ createdAt: -1 })
      .populate('contentId', 'title posterUrl bannerUrl contentType contentRating genres releaseYear duration rating');
  }

  async isInCollection(userId: string, profileId: string, contentId: string): Promise<boolean> {
    const item = await this.collectionModel.findOne({
      userId: new Types.ObjectId(userId),
      profileId: new Types.ObjectId(profileId),
      contentId: new Types.ObjectId(contentId),
    });
    return !!item;
  }
}
