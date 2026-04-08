import { IsNotEmpty, IsString } from 'class-validator';

export class CreateRazorpayOrderDto {
  @IsString()
  @IsNotEmpty()
  planId: string; // '1m', '3m', '6m', '12m'
}

export class VerifyRazorpayPaymentDto {
  @IsString()
  @IsNotEmpty()
  razorpay_payment_id: string;

  @IsString()
  @IsNotEmpty()
  razorpay_order_id: string;

  @IsString()
  @IsNotEmpty()
  razorpay_signature: string;
}
