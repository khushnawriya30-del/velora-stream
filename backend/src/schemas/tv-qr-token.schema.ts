import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type TvQrTokenDocument = TvQrToken & Document;

@Schema({ timestamps: true })
export class TvQrToken {
  @Prop({ required: true, unique: true, index: true })
  token: string;

  @Prop({ type: String, enum: ['pending', 'approved', 'expired'], default: 'pending' })
  status: string;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  approvedByUserId: Types.ObjectId;

  @Prop({ required: true })
  expiresAt: Date;
}

export const TvQrTokenSchema = SchemaFactory.createForClass(TvQrToken);

// TTL index: auto-delete expired tokens
TvQrTokenSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });
