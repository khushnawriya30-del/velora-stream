import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type AppVersionDocument = AppVersion & Document;

@Schema({ timestamps: true })
export class AppVersion {
  @Prop({ required: true, default: 1 })
  versionCode: number;

  @Prop({ required: true, default: '1.0.0' })
  versionName: string;

  @Prop({ default: false })
  forceUpdate: boolean;

  @Prop({ default: '' })
  apkUrl: string;

  @Prop({ default: '' })
  releaseNotes: string;
}

export const AppVersionSchema = SchemaFactory.createForClass(AppVersion);
