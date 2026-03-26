import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ProfileDocument = Profile & Document;

export enum MaturityRating {
  G = 'G',
  PG = 'PG',
  A = 'A',
}

@Schema({ timestamps: true })
export class Profile {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ required: true, trim: true })
  displayName: string;

  @Prop()
  avatarUrl: string;

  @Prop({ type: String, enum: MaturityRating, default: MaturityRating.PG })
  maturityRating: MaturityRating;

  @Prop()
  pin: string;

  @Prop({ default: true })
  isActive: boolean;
}

export const ProfileSchema = SchemaFactory.createForClass(Profile);
ProfileSchema.index({ userId: 1 });
