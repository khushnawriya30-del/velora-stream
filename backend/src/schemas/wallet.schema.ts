import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type WalletDocument = Wallet & Document;

@Schema({ timestamps: true })
export class Wallet {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true, unique: true })
  userId: Types.ObjectId;

  @Prop({ default: 80 })
  balance: number;

  @Prop({ default: 100 })
  withdrawThreshold: number;

  @Prop({ default: 80 })
  totalEarned: number;

  @Prop({ default: 0 })
  totalWithdrawn: number;

  @Prop({ default: 0 })
  totalReferrals: number;

  // Saved bank details (filled once by user)
  @Prop({ default: '' })
  bankName: string;

  @Prop({ default: '' })
  accountNumber: string;

  @Prop({ default: '' })
  ifscCode: string;

  @Prop({ default: '' })
  accountHolderName: string;

  @Prop({ default: '' })
  phoneNumber: string;

  @Prop({ default: '' })
  bankEmail: string;
}

export const WalletSchema = SchemaFactory.createForClass(Wallet);
WalletSchema.index({ userId: 1 }, { unique: true });
