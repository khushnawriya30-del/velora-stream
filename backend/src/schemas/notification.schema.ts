import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type NotificationDocument = Notification & Document;

@Schema({ timestamps: true })
export class Notification {
  @Prop({ required: true })
  title: string;

  @Prop({ required: true })
  body: string;

  @Prop()
  imageUrl: string;

  @Prop()
  deepLink: string;

  @Prop()
  targetAudience: string; // 'all' | 'genre:action' | 'subscription:premium'

  @Prop({ type: [Types.ObjectId], ref: 'User' })
  targetUsers: Types.ObjectId[];

  @Prop()
  scheduledAt: Date;

  @Prop({ default: false })
  isSent: boolean;

  @Prop()
  sentAt: Date;

  @Prop({ default: 0 })
  deliveredCount: number;

  @Prop({ default: 0 })
  openedCount: number;

  @Prop()
  createdBy: string;
}

export const NotificationSchema = SchemaFactory.createForClass(Notification);
NotificationSchema.index({ isSent: 1, scheduledAt: 1 });
