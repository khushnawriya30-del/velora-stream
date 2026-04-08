import {
  Injectable,
  BadRequestException,
  Logger,
  InternalServerErrorException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import * as crypto from 'crypto';
import {
  RazorpayPayment,
  RazorpayPaymentDocument,
  RazorpayPaymentStatus,
} from '../../schemas/razorpay-payment.schema';
import { User, UserDocument } from '../../schemas/user.schema';
import {
  PremiumPlanConfig,
  PremiumPlanConfigDocument,
} from '../../schemas/premium-plan.schema';
import {
  CreateRazorpayOrderDto,
  VerifyRazorpayPaymentDto,
} from './dto/razorpay.dto';

// Plan duration mapping
const PLAN_DURATION: Record<string, number> = {
  '1m': 30,
  '3m': 90,
  '6m': 180,
  '12m': 365,
};

const PLAN_NAMES: Record<string, string> = {
  '1m': '1 Month Premium',
  '3m': '3 Months Premium',
  '6m': '6 Months Premium',
  '12m': '1 Year Premium',
};

@Injectable()
export class RazorpayService {
  private readonly logger = new Logger(RazorpayService.name);
  private readonly keyId: string;
  private readonly keySecret: string;
  private readonly baseUrl = 'https://api.razorpay.com/v1';

  constructor(
    private configService: ConfigService,
    @InjectModel(RazorpayPayment.name)
    private razorpayPaymentModel: Model<RazorpayPaymentDocument>,
    @InjectModel(User.name)
    private userModel: Model<UserDocument>,
    @InjectModel(PremiumPlanConfig.name)
    private planModel: Model<PremiumPlanConfigDocument>,
  ) {
    this.keyId = this.configService.get<string>('RAZORPAY_KEY_ID', '');
    this.keySecret = this.configService.get<string>('RAZORPAY_KEY_SECRET', '');
  }

  /**
   * Create a Razorpay order using the Orders API
   */
  async createOrder(userId: string, dto: CreateRazorpayOrderDto) {
    const { planId } = dto;

    if (!PLAN_DURATION[planId]) {
      throw new BadRequestException('Invalid plan ID. Use: 1m, 3m, 6m, 12m');
    }

    // Check for existing unpaid orders (prevent spam)
    const recentPending = await this.razorpayPaymentModel.countDocuments({
      userId: new Types.ObjectId(userId),
      status: RazorpayPaymentStatus.CREATED,
      createdAt: { $gte: new Date(Date.now() - 60 * 60 * 1000) }, // last 1 hour
    });

    if (recentPending >= 5) {
      throw new BadRequestException(
        'Too many pending orders. Please wait or complete existing payment.',
      );
    }

    // Get plan price from DB (allows easy price changes)
    let amountInPaise: number;
    const dbPlan = await this.planModel.findOne({
      planId,
      isActive: true,
    });

    if (dbPlan) {
      amountInPaise = dbPlan.price * 100; // DB stores in rupees
    } else {
      // Fallback: ₹1 for testing
      amountInPaise = 100;
    }

    // Create Razorpay order via API
    const auth = Buffer.from(`${this.keyId}:${this.keySecret}`).toString(
      'base64',
    );

    const orderPayload = {
      amount: amountInPaise,
      currency: 'INR',
      receipt: `rcpt_${userId}_${Date.now()}`,
      notes: {
        userId,
        planId,
        planName: PLAN_NAMES[planId],
      },
    };

    let razorpayOrder: any;
    try {
      const response = await fetch(`${this.baseUrl}/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Basic ${auth}`,
        },
        body: JSON.stringify(orderPayload),
      });

      if (!response.ok) {
        const error = await response.text();
        this.logger.error(`Razorpay order creation failed: ${error}`);
        throw new InternalServerErrorException('Failed to create payment order');
      }

      razorpayOrder = await response.json();
    } catch (error) {
      if (error instanceof InternalServerErrorException) throw error;
      this.logger.error(`Razorpay API error: ${error.message}`);
      throw new InternalServerErrorException('Payment service unavailable');
    }

    // Save order in DB
    const payment = await this.razorpayPaymentModel.create({
      orderId: razorpayOrder.id,
      userId: new Types.ObjectId(userId),
      plan: planId,
      planName: PLAN_NAMES[planId],
      amount: amountInPaise,
      currency: 'INR',
      durationDays: PLAN_DURATION[planId],
      status: RazorpayPaymentStatus.CREATED,
    });

    return {
      orderId: razorpayOrder.id,
      amount: amountInPaise,
      currency: 'INR',
      keyId: this.keyId,
      planName: PLAN_NAMES[planId],
      plan: planId,
    };
  }

  /**
   * Verify Razorpay payment signature and activate premium
   */
  async verifyPayment(userId: string, dto: VerifyRazorpayPaymentDto) {
    const { razorpay_payment_id, razorpay_order_id, razorpay_signature } = dto;

    // 1. Find the order
    const payment = await this.razorpayPaymentModel.findOne({
      orderId: razorpay_order_id,
      userId: new Types.ObjectId(userId),
    });

    if (!payment) {
      throw new BadRequestException('Order not found');
    }

    // 2. Prevent duplicate activation
    if (payment.status === RazorpayPaymentStatus.PAID && payment.premiumActivated) {
      return {
        success: true,
        message: 'Payment already verified and premium activated',
        alreadyActivated: true,
      };
    }

    // 3. Verify signature using HMAC SHA256
    const expectedSignature = crypto
      .createHmac('sha256', this.keySecret)
      .update(`${razorpay_order_id}|${razorpay_payment_id}`)
      .digest('hex');

    if (expectedSignature !== razorpay_signature) {
      // Mark as failed
      await this.razorpayPaymentModel.updateOne(
        { _id: payment._id },
        {
          status: RazorpayPaymentStatus.FAILED,
          failedAt: new Date(),
          failureReason: 'Invalid signature - potential fraud',
        },
      );
      this.logger.warn(
        `FRAUD ALERT: Invalid signature for order ${razorpay_order_id}, user ${userId}`,
      );
      throw new BadRequestException('Payment verification failed - invalid signature');
    }

    // 4. Signature valid → activate premium
    const now = new Date();
    const expiryDate = new Date(now);
    expiryDate.setDate(expiryDate.getDate() + payment.durationDays);

    // Update payment record
    await this.razorpayPaymentModel.updateOne(
      { _id: payment._id },
      {
        status: RazorpayPaymentStatus.PAID,
        razorpayPaymentId: razorpay_payment_id,
        razorpaySignature: razorpay_signature,
        verifiedAt: now,
        premiumActivated: true,
      },
    );

    // 5. Activate premium on user
    const premiumPlanMap: Record<string, string> = {
      '1m': '1month',
      '3m': '3months',
      '6m': '6months',
      '12m': '1year',
    };

    await this.userModel.updateOne(
      { _id: new Types.ObjectId(userId) },
      {
        isPremium: true,
        premiumPlan: premiumPlanMap[payment.plan] || payment.plan,
        premiumActivatedAt: now,
        premiumExpiresAt: expiryDate,
        maxDevices: 2,
      },
    );

    this.logger.log(
      `Premium activated: user=${userId}, plan=${payment.plan}, expires=${expiryDate.toISOString()}`,
    );

    return {
      success: true,
      message: 'Payment verified! Premium activated.',
      premiumPlan: premiumPlanMap[payment.plan],
      premiumExpiresAt: expiryDate.toISOString(),
      daysRemaining: payment.durationDays,
    };
  }

  /**
   * Webhook handler: payment.captured, order.paid, payment.failed events
   */
  async handleWebhook(rawBody: string, parsedBody: any, signature: string) {
    const webhookSecret = this.configService.get<string>(
      'RAZORPAY_WEBHOOK_SECRET',
      '',
    );

    if (!webhookSecret) {
      this.logger.warn('Webhook secret not configured');
      return { status: 'ignored' };
    }

    // Verify webhook signature using raw body string
    const expectedSignature = crypto
      .createHmac('sha256', webhookSecret)
      .update(rawBody)
      .digest('hex');

    if (expectedSignature !== signature) {
      this.logger.warn('Invalid webhook signature');
      throw new BadRequestException('Invalid webhook signature');
    }

    const event = parsedBody.event;
    this.logger.log(`Webhook received: ${event}`);

    // Handle payment.failed
    if (event === 'payment.failed') {
      const paymentEntity = parsedBody.payload?.payment?.entity;
      if (paymentEntity?.order_id) {
        await this.razorpayPaymentModel.updateOne(
          { orderId: paymentEntity.order_id },
          {
            status: RazorpayPaymentStatus.FAILED,
            failedAt: new Date(),
            failureReason:
              paymentEntity.error_description || 'Payment failed via webhook',
          },
        );
        this.logger.log(
          `Webhook: Payment failed for order=${paymentEntity.order_id}`,
        );
      }
      return { status: 'payment_failed_recorded' };
    }

    // Handle payment.captured and order.paid
    const paymentEntity =
      event === 'order.paid'
        ? parsedBody.payload?.order?.entity
        : parsedBody.payload?.payment?.entity;

    if (
      (event === 'payment.captured' || event === 'order.paid') &&
      paymentEntity
    ) {
      const razorpayOrderId =
        event === 'order.paid' ? paymentEntity.id : paymentEntity.order_id;
      const razorpayPaymentId =
        event === 'order.paid'
          ? parsedBody.payload?.payment?.entity?.id
          : paymentEntity.id;

      const payment = await this.razorpayPaymentModel.findOne({
        orderId: razorpayOrderId,
      });

      if (!payment) {
        this.logger.warn(`Webhook: Order ${razorpayOrderId} not found`);
        return { status: 'order_not_found' };
      }

      // If already activated via verify endpoint, skip
      if (payment.premiumActivated) {
        return { status: 'already_activated' };
      }

      // Activate premium via webhook (backup path)
      const now = new Date();
      const expiryDate = new Date(now);
      expiryDate.setDate(expiryDate.getDate() + payment.durationDays);

      const premiumPlanMap: Record<string, string> = {
        '1m': '1month',
        '3m': '3months',
        '6m': '6months',
        '12m': '1year',
      };

      await this.razorpayPaymentModel.updateOne(
        { _id: payment._id },
        {
          status: RazorpayPaymentStatus.PAID,
          razorpayPaymentId,
          verifiedAt: now,
          premiumActivated: true,
        },
      );

      await this.userModel.updateOne(
        { _id: payment.userId },
        {
          isPremium: true,
          premiumPlan: premiumPlanMap[payment.plan] || payment.plan,
          premiumActivatedAt: now,
          premiumExpiresAt: expiryDate,
          maxDevices: 2,
        },
      );

      this.logger.log(
        `Webhook: Premium activated for user=${payment.userId}, order=${razorpayOrderId}`,
      );

      return { status: 'activated' };
    }

    return { status: 'event_ignored' };
  }

  /**
   * Get payment history for a user
   */
  async getPaymentHistory(userId: string) {
    return this.razorpayPaymentModel
      .find({ userId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .limit(20)
      .select('orderId plan planName amount status createdAt verifiedAt razorpayPaymentId')
      .lean();
  }
}
