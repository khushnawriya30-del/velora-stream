import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type UpiPaymentDocument = UpiPayment & Document;

export enum UpiPaymentStatus {
  PENDING = 'pending',
  UTR_SUBMITTED = 'utr_submitted',
  VERIFIED = 'verified',
  REJECTED = 'rejected',
  EXPIRED = 'expired',
}

@Schema({ timestamps: true })
export class UpiPayment {
  @Prop({ required: true })
  orderId: string; // VELORA-xxxxxxxxxx unique order ID

  @Prop({ type: Types.ObjectId, ref: 'User' })
  userId: Types.ObjectId; // Logged-in user (optional for guest)

  @Prop({ required: true })
  plan: string; // '1month', '3months', '6months', '1year'

  @Prop({ required: true })
  planName: string;

  @Prop({ required: true })
  amount: number;

  @Prop()
  upiId: string; // UPI ID payment was made to

  @Prop({ uppercase: true, trim: true })
  utrId: string; // User-submitted UTR

  @Prop({ type: String, enum: UpiPaymentStatus, default: UpiPaymentStatus.PENDING })
  status: UpiPaymentStatus;

  @Prop()
  activationCode: string;

  @Prop({ default: false })
  isCodeRedeemed: boolean;

  @Prop()
  verifiedAt: Date;

  @Prop()
  rejectedAt: Date;

  @Prop()
  rejectionReason: string;

  @Prop()
  expiresAt: Date; // Order expires after 30 min if no UTR submitted

  @Prop()
  deviceInfo: string; // For fraud detection

  @Prop()
  note: string;
}

export const UpiPaymentSchema = SchemaFactory.createForClass(UpiPayment);
UpiPaymentSchema.index({ orderId: 1 }, { unique: true });
UpiPaymentSchema.index({ utrId: 1 }, { sparse: true });
UpiPaymentSchema.index({ userId: 1 });
UpiPaymentSchema.index({ status: 1 });
UpiPaymentSchema.index({ createdAt: -1 });
