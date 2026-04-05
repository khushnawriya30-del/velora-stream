import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type TelegramPaymentDocument = TelegramPayment & Document;

export enum PaymentStatus {
  PENDING = 'pending',
  VERIFIED = 'verified',
  REJECTED = 'rejected',
  EXPIRED = 'expired',
}

@Schema({ timestamps: true })
export class TelegramPayment {
  @Prop({ required: true })
  telegramUserId: string;

  @Prop()
  telegramUsername: string;

  @Prop()
  telegramFirstName: string;

  @Prop({ required: true })
  plan: string; // '1month', '3months', '6months', '1year'

  @Prop({ required: true })
  amount: number;

  @Prop({ required: true, unique: true, uppercase: true, trim: true })
  utrId: string;

  @Prop({ type: String, enum: PaymentStatus, default: PaymentStatus.PENDING })
  status: PaymentStatus;

  @Prop()
  activationCode: string; // Generated after verification

  @Prop({ default: false })
  isCodeRedeemed: boolean;

  @Prop()
  verifiedAt: Date;

  @Prop()
  rejectedAt: Date;

  @Prop()
  rejectionReason: string;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  redeemedByUser: Types.ObjectId;

  @Prop()
  note: string; // Admin note
}

export const TelegramPaymentSchema =
  SchemaFactory.createForClass(TelegramPayment);
TelegramPaymentSchema.index({ utrId: 1 }, { unique: true });
TelegramPaymentSchema.index({ telegramUserId: 1 });
TelegramPaymentSchema.index({ status: 1 });
TelegramPaymentSchema.index({ activationCode: 1 });
