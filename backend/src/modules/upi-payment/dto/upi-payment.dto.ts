import { IsString, IsOptional, IsNumber } from 'class-validator';

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

export class VerifyPaymentDto {
  @IsString()
  orderId: string;

  @IsString()
  status: string; // 'SUCCESS', 'FAILURE', 'SUBMITTED'

  @IsOptional()
  @IsString()
  txnId?: string; // UPI transaction ID returned by payment app

  @IsOptional()
  @IsString()
  responseCode?: string;

  @IsOptional()
  @IsString()
  approvalRefNo?: string;
}
