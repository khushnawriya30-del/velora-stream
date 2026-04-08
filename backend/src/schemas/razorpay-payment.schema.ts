import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type RazorpayPaymentDocument = RazorpayPayment & Document;

export enum RazorpayPaymentStatus {
  CREATED = 'created',
  PAID = 'paid',
  FAILED = 'failed',
  EXPIRED = 'expired',
}

@Schema({ timestamps: true })
export class RazorpayPayment {
  @Prop({ required: true, unique: true })
  orderId: string; // Razorpay order_id

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  userId: Types.ObjectId;

  @Prop({ required: true })
  plan: string; // '1m', '3m', '6m', '12m'

  @Prop({ required: true })
  planName: string;

  @Prop({ required: true })
  amount: number; // in paise (₹10 = 1000)

  @Prop({ default: 'INR' })
  currency: string;

  @Prop({ type: String, enum: RazorpayPaymentStatus, default: RazorpayPaymentStatus.CREATED })
  status: RazorpayPaymentStatus;

  @Prop()
  razorpayPaymentId: string;

  @Prop()
  razorpaySignature: string;

  @Prop()
  durationDays: number;

  @Prop()
  verifiedAt: Date;

  @Prop()
  failedAt: Date;

  @Prop()
  failureReason: string;

  @Prop({ default: false })
  premiumActivated: boolean;
}

export const RazorpayPaymentSchema = SchemaFactory.createForClass(RazorpayPayment);
RazorpayPaymentSchema.index({ orderId: 1 }, { unique: true });
RazorpayPaymentSchema.index({ userId: 1 });
RazorpayPaymentSchema.index({ status: 1 });
RazorpayPaymentSchema.index({ razorpayPaymentId: 1 }, { sparse: true });
RazorpayPaymentSchema.index({ createdAt: -1 });
