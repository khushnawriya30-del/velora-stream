import { IsString, IsNumber, IsBoolean, IsOptional, IsEnum } from 'class-validator';

export class CreatePremiumOfferDto {
  @IsString()
  title: string;

  @IsOptional()
  @IsString()
  subtitle?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsNumber()
  originalPrice: number;

  @IsNumber()
  discountPrice: number;

  @IsOptional()
  @IsString()
  badgeText?: string;

  @IsOptional()
  @IsString()
  planId?: string;

  @IsOptional()
  @IsNumber()
  durationMonths?: number;

  @IsOptional()
  @IsEnum(['non_premium', 'premium', 'all'])
  targetUserType?: string;

  @IsOptional()
  @IsString()
  offerType?: string;

  @IsOptional()
  @IsBoolean()
  isVisible?: boolean;

  @IsOptional()
  @IsBoolean()
  showAsPopup?: boolean;

  @IsOptional()
  @IsNumber()
  order?: number;

  @IsOptional()
  @IsString()
  startDate?: string;

  @IsOptional()
  @IsString()
  endDate?: string;
}

export class UpdatePremiumOfferDto extends CreatePremiumOfferDto {}

export class UpdateInviteSettingsDto {
  @IsOptional()
  @IsNumber()
  targetAmount?: number;

  @IsOptional()
  @IsNumber()
  defaultBalance?: number;

  @IsOptional()
  @IsNumber()
  rewardPerInvite?: number;

  @IsOptional()
  @IsNumber()
  earnWindowDays?: number;

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;
}
