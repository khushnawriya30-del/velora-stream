import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type WatchlistDocument = Watchlist & Document;

@Schema({ timestamps: true })
export class Watchlist {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Profile', required: true })
  profileId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Movie', required: true })
  contentId: Types.ObjectId;
}

export const WatchlistSchema = SchemaFactory.createForClass(Watchlist);
WatchlistSchema.index(
  { userId: 1, profileId: 1, contentId: 1 },
  { unique: true },
);
