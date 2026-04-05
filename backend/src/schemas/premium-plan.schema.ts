import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type PremiumPlanConfigDocument = PremiumPlanConfig & Document;

@Schema({ timestamps: true })
export class PremiumPlanConfig {
  @Prop({ required: true, unique: true })
  planId: string; // e.g. "1m", "3m", "6m", "12m"

  @Prop({ required: true })
  name: string; // e.g. "1 Month"

  @Prop({ required: true })
  months: number;

  @Prop({ required: true })
  price: number;

  @Prop({ required: true })
  originalPrice: number;

  @Prop({ required: true })
  discountPercent: number;

  @Prop()
  badge: string; // e.g. "Most popular", "Best Value"

  @Prop({ default: 0 })
  order: number; // display order

  @Prop({ default: true })
  isActive: boolean;
}

export const PremiumPlanConfigSchema =
  SchemaFactory.createForClass(PremiumPlanConfig);
PremiumPlanConfigSchema.index({ planId: 1 }, { unique: true });
PremiumPlanConfigSchema.index({ order: 1 });
