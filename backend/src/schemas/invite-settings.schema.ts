import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type InviteSettingsDocument = InviteSettings & Document;

@Schema({ timestamps: true })
export class InviteSettings {
  @Prop({ default: 'default', unique: true })
  key: string;

  @Prop({ default: 100 })
  targetAmount: number;

  @Prop({ default: 80 })
  defaultBalance: number;

  @Prop({ default: 1 })
  rewardPerInvite: number;

  @Prop({ default: 60 })
  earnWindowDays: number;

  @Prop({ default: true })
  isActive: boolean;
}

export const InviteSettingsSchema = SchemaFactory.createForClass(InviteSettings);
