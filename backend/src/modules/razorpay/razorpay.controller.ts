import {
  Controller,
  Post,
  Get,
  Body,
  Req,
  Headers,
  UseGuards,
  HttpCode,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RazorpayService } from './razorpay.service';
import {
  CreateRazorpayOrderDto,
  VerifyRazorpayPaymentDto,
} from './dto/razorpay.dto';

@Controller('razorpay')
export class RazorpayController {
  constructor(private readonly razorpayService: RazorpayService) {}

  /**
   * POST /razorpay/create-order
   * Create a Razorpay order for the authenticated user
   */
  @Post('create-order')
  @UseGuards(AuthGuard('jwt'))
  async createOrder(
    @CurrentUser('userId') userId: string,
    @Body() dto: CreateRazorpayOrderDto,
  ) {
    return this.razorpayService.createOrder(userId, dto);
  }

  /**
   * POST /razorpay/verify-payment
   * Verify Razorpay payment signature and activate premium
   */
  @Post('verify-payment')
  @UseGuards(AuthGuard('jwt'))
  async verifyPayment(
    @CurrentUser('userId') userId: string,
    @Body() dto: VerifyRazorpayPaymentDto,
  ) {
    return this.razorpayService.verifyPayment(userId, dto);
  }

  /**
   * POST /razorpay/webhook
   * Razorpay webhook handler (no auth guard — Razorpay calls this)
   */
  @Post('webhook')
  @HttpCode(200)
  async webhook(
    @Req() req: any,
    @Body() body: any,
    @Headers('x-razorpay-signature') signature: string,
  ) {
    // Use raw body for signature verification (JSON.stringify may differ)
    const rawBody = req.rawBody
      ? req.rawBody.toString('utf8')
      : JSON.stringify(body);
    return this.razorpayService.handleWebhook(rawBody, body, signature);
  }

  /**
   * GET /razorpay/history
   * Get payment history for the authenticated user
   */
  @Get('history')
  @UseGuards(AuthGuard('jwt'))
  async getHistory(@CurrentUser('userId') userId: string) {
    return this.razorpayService.getPaymentHistory(userId);
  }
}
