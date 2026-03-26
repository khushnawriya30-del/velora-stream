import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type WatchProgressDocument = WatchProgress & Document;

@Schema({ timestamps: true })
export class WatchProgress {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Profile', required: true })
  profileId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, required: true })
  contentId: Types.ObjectId; // Movie or Episode

  @Prop({ required: true })
  contentType: string; // 'movie' | 'episode'

  @Prop({ type: Types.ObjectId })
  seriesId: Types.ObjectId; // for episodes

  @Prop({ default: 0 })
  currentTime: number; // seconds

  @Prop({ default: 0 })
  totalDuration: number; // seconds

  @Prop({ default: false })
  isCompleted: boolean; // >=85% watched

  @Prop()
  lastWatchedAt: Date;

  @Prop()
  episodeTitle: string;

  @Prop()
  contentTitle: string;

  @Prop()
  thumbnailUrl: string;
}

export const WatchProgressSchema = SchemaFactory.createForClass(WatchProgress);
WatchProgressSchema.index(
  { userId: 1, profileId: 1, contentId: 1 },
  { unique: true },
);
WatchProgressSchema.index({ userId: 1, profileId: 1, lastWatchedAt: -1 });
