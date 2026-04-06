import { IsOptional, IsString } from 'class-validator';

export class UpdateSettingsDto {
  @IsOptional()
  @IsString()
  tmdbAccessToken?: string;

  @IsOptional()
  @IsString()
  telegramBotToken?: string;

  @IsOptional()
  @IsString()
  telegramBotUsername?: string;

  @IsOptional()
  @IsString()
  paymentQrCodeUrl?: string;

  @IsOptional()
  @IsString()
  paymentUpiId?: string;

  @IsOptional()
  @IsString()
  paymentInstructions?: string;
}
