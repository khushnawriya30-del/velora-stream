import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ThematicCollectionDocument = ThematicCollection & Document;

@Schema({ timestamps: true })
export class ThematicCollection {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Profile', required: true })
  profileId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Movie', required: true })
  contentId: Types.ObjectId;
}

export const ThematicCollectionSchema = SchemaFactory.createForClass(ThematicCollection);
ThematicCollectionSchema.index(
  { userId: 1, profileId: 1, contentId: 1 },
  { unique: true },
);
