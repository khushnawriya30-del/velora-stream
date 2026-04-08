import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type PremiumOfferDocument = PremiumOffer & Document;

@Schema({ timestamps: true })
export class PremiumOffer {
  @Prop({ required: true })
  title: string;

  @Prop({ default: '' })
  subtitle: string;

  @Prop({ default: '' })
  description: string;

  @Prop({ default: '' })
  bannerText: string;

  @Prop({ required: true })
  originalPrice: number;

  @Prop({ required: true })
  discountPrice: number;

  @Prop({ default: 0 })
  discountPercent: number;

  @Prop({ default: '' })
  badgeText: string;

  @Prop({ default: '1m' })
  planId: string;

  @Prop({ default: 1 })
  durationMonths: number;

  @Prop({ enum: ['non_premium', 'premium', 'all'], default: 'non_premium' })
  targetUserType: string;

  @Prop({ default: 'subscription' })
  offerType: string; // subscription, renewal, upgrade, retention

  @Prop({ default: true })
  isVisible: boolean;

  @Prop({ default: false })
  showAsPopup: boolean;

  @Prop({ default: 0 })
  order: number;

  @Prop()
  startDate: Date;

  @Prop()
  endDate: Date;
}

export const PremiumOfferSchema = SchemaFactory.createForClass(PremiumOffer);
