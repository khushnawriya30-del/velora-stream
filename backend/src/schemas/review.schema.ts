import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ReviewDocument = Review & Document;

export enum ModerationStatus {
  PENDING = 'pending',
  APPROVED = 'approved',
  REJECTED = 'rejected',
}

@Schema({ timestamps: true })
export class Review {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Movie', required: true })
  contentId: Types.ObjectId;

  @Prop({ min: 1, max: 10 })
  rating: number;

  @Prop({ maxlength: 2000 })
  text: string;

  @Prop({ type: String, enum: ModerationStatus, default: ModerationStatus.PENDING })
  moderationStatus: ModerationStatus;

  @Prop()
  moderatedBy: string;

  @Prop()
  moderatedAt: Date;
}

export const ReviewSchema = SchemaFactory.createForClass(Review);
ReviewSchema.index({ contentId: 1, moderationStatus: 1 });
ReviewSchema.index({ userId: 1, contentId: 1 }, { unique: true });
