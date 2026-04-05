import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type AppSettingsDocument = AppSettings & Document;

@Schema({ timestamps: true })
export class AppSettings {
  @Prop({ default: '' })
  tmdbAccessToken: string;

  // ── Telegram Bot Settings ──
  @Prop({ default: '' })
  telegramBotToken: string;

  @Prop({ default: '' })
  telegramBotUsername: string; // e.g. "VeloraPremiumBot"

  @Prop({ default: '' })
  paymentQrCodeUrl: string; // URL to UPI QR code image

  @Prop({ default: '' })
  paymentUpiId: string; // e.g. "yourname@upi"

  @Prop({ default: '' })
  paymentInstructions: string; // Custom instructions shown after QR
}

export const AppSettingsSchema = SchemaFactory.createForClass(AppSettings);
