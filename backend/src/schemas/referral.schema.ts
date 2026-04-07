import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ReferralDocument = Referral & Document;

export enum ReferralStatus {
  SUCCESS = 'success',
  PENDING = 'pending',
}

@Schema({ timestamps: true })
export class Referral {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  referrerId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  newUserId: Types.ObjectId;

  @Prop({ default: 1 })
  amount: number;

  @Prop({ type: String, enum: ReferralStatus, default: ReferralStatus.SUCCESS })
  status: ReferralStatus;
}

export const ReferralSchema = SchemaFactory.createForClass(Referral);
ReferralSchema.index({ referrerId: 1 });
ReferralSchema.index({ newUserId: 1 }, { unique: true });
