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
}

export const WalletSchema = SchemaFactory.createForClass(Wallet);
WalletSchema.index({ userId: 1 }, { unique: true });
