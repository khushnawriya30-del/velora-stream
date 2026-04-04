import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ActivationCodeDocument = ActivationCode & Document;

export enum PremiumPlan {
  ONE_MONTH = '1month',
  THREE_MONTHS = '3months',
  SIX_MONTHS = '6months',
  ONE_YEAR = '1year',
}

@Schema({ timestamps: true })
export class ActivationCode {
  @Prop({ required: true, unique: true, uppercase: true })
  code: string;

  @Prop({ type: String, enum: PremiumPlan, required: true })
  plan: PremiumPlan;

  @Prop({ required: true })
  durationDays: number; // 30, 90, 180, 365

  @Prop({ default: false })
  isRedeemed: boolean;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  redeemedBy: Types.ObjectId;

  @Prop()
  redeemedAt: Date;

  @Prop()
  expiresAt: Date; // Code itself expires (unused codes expire after 30 days)

  @Prop({ default: false })
  isRevoked: boolean;

  @Prop()
  revokedReason: string;

  @Prop()
  batchId: string; // For grouping bulk-generated codes

  @Prop()
  note: string; // Admin note (e.g., "giveaway", "influencer promo")
}

export const ActivationCodeSchema =
  SchemaFactory.createForClass(ActivationCode);
ActivationCodeSchema.index({ code: 1 }, { unique: true });
ActivationCodeSchema.index({ isRedeemed: 1 });
ActivationCodeSchema.index({ plan: 1 });
ActivationCodeSchema.index({ batchId: 1 });
