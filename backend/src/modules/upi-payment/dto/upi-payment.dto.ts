import { IsString, IsOptional } from 'class-validator';

export class CreateOrderDto {
  @IsString()
  planId: string; // '1m', '3m', '6m', '12m'

  @IsOptional()
  @IsString()
  deviceInfo?: string;
}

export class SubmitUtrDto {
  @IsString()
  orderId: string;

  @IsString()
  utrId: string;
}
