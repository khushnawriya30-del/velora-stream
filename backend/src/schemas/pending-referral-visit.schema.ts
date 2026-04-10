import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type PendingReferralVisitDocument = PendingReferralVisit & Document;

@Schema({ timestamps: true })
export class PendingReferralVisit {
  @Prop({ required: true })
  referralCode: string;

  @Prop({ required: true })
  ipAddress: string;

  @Prop()
  userAgent: string;

  @Prop({ default: Date.now, expires: 86400 }) // Auto-delete after 24 hours
  createdAt: Date;
}

export const PendingReferralVisitSchema = SchemaFactory.createForClass(PendingReferralVisit);
PendingReferralVisitSchema.index({ ipAddress: 1, createdAt: -1 });
PendingReferralVisitSchema.index({ createdAt: 1 }, { expireAfterSeconds: 86400 });
