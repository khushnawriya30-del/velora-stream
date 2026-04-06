import {
  Injectable,
  BadRequestException,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import * as crypto from 'crypto';
import {
  UpiPayment,
  UpiPaymentDocument,
  UpiPaymentStatus,
} from '../../schemas/upi-payment.schema';
import {
  PremiumPlanConfig,
  PremiumPlanConfigDocument,
} from '../../schemas/premium-plan.schema';
import { PremiumPlan } from '../../schemas/activation-code.schema';
import { SettingsService } from '../settings/settings.service';
import { PremiumService } from '../premium/premium.service';

@Injectable()
export class UpiPaymentService {
  private readonly logger = new Logger(UpiPaymentService.name);

  private readonly planEnumMap: Record<string, string> = {
    '1m': '1month',
    '3m': '3months',
    '6m': '6months',
    '12m': '1year',
  };

  constructor(
    @InjectModel(UpiPayment.name)
    private paymentModel: Model<UpiPaymentDocument>,
    @InjectModel(PremiumPlanConfig.name)
    private planModel: Model<PremiumPlanConfigDocument>,
    private readonly settingsService: SettingsService,
    private readonly premiumService: PremiumService,
  ) {}

  // ══════════════════════════════
  //  CREATE ORDER + UPI LINK
  // ══════════════════════════════

  async createOrder(planId: string, userId?: string, deviceInfo?: string) {
    const plan = await this.planModel
      .findOne({ planId, isActive: true })
      .lean();
    if (!plan) throw new BadRequestException('Invalid plan');

    const settings = await this.settingsService.getSettings();
    const upiId = settings.paymentUpiId;
    if (!upiId) throw new BadRequestException('Payment not configured');

    // Rate limit: Max 5 pending orders per user per hour
    if (userId) {
      const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
      const recentCount = await this.paymentModel.countDocuments({
        userId: new Types.ObjectId(userId),
        status: { $in: [UpiPaymentStatus.PENDING, UpiPaymentStatus.UTR_SUBMITTED] },
        createdAt: { $gte: oneHourAgo },
      });
      if (recentCount >= 5) {
        throw new BadRequestException(
          'Too many pending orders. Please complete or wait for existing orders.',
        );
      }
    }

    // Generate unique order ID
    const orderId = `VELORA-${Date.now().toString(36).toUpperCase()}${crypto.randomBytes(3).toString('hex').toUpperCase()}`;

    // Order expires in 30 minutes
    const expiresAt = new Date(Date.now() + 30 * 60 * 1000);

    const payment = await this.paymentModel.create({
      orderId,
      userId: userId ? new Types.ObjectId(userId) : undefined,
      plan: this.planEnumMap[plan.planId] || plan.planId,
      planName: plan.name,
      amount: plan.price,
      upiId,
      status: UpiPaymentStatus.PENDING,
      expiresAt,
      deviceInfo,
    });

    // Build UPI deep link
    const upiLink =
      `upi://pay?pa=${encodeURIComponent(upiId)}` +
      `&pn=${encodeURIComponent('Velora')}` +
      `&am=${plan.price}` +
      `&cu=INR` +
      `&tn=${encodeURIComponent(`Velora Premium ${plan.name}`)}`;

    return {
      orderId: payment.orderId,
      plan: plan.name,
      amount: plan.price,
      upiId,
      upiLink,
      expiresAt,
    };
  }

  // ══════════════════════════════
  //  SUBMIT UTR
  // ══════════════════════════════

  async submitUtr(orderId: string, utrId: string, userId?: string) {
    const cleanUtr = utrId.trim().toUpperCase();

    // Validate UTR format (6-30 alphanumeric chars)
    if (!/^[A-Z0-9]{6,30}$/.test(cleanUtr)) {
      throw new BadRequestException(
        'Invalid UTR format. Enter a valid 6-30 character alphanumeric Transaction ID.',
      );
    }

    // Find the order
    const order = await this.paymentModel.findOne({ orderId });
    if (!order) throw new NotFoundException('Order not found');

    // Verify ownership if userId provided
    if (userId && order.userId && order.userId.toString() !== userId) {
      throw new BadRequestException('Order does not belong to you');
    }

    // Check order isn't already processed
    if (order.status === UpiPaymentStatus.VERIFIED) {
      throw new BadRequestException('This order is already verified');
    }
    if (order.status === UpiPaymentStatus.REJECTED) {
      throw new BadRequestException('This order was rejected');
    }

    // Check order hasn't expired
    if (order.expiresAt && new Date() > order.expiresAt) {
      order.status = UpiPaymentStatus.EXPIRED;
      await order.save();
      throw new BadRequestException(
        'Order has expired. Please create a new order.',
      );
    }

    // Check UTR not already used (across ALL payment tables)
    const existingUpi = await this.paymentModel.findOne({
      utrId: cleanUtr,
      _id: { $ne: order._id },
    });
    if (existingUpi) {
      throw new BadRequestException(
        'This UTR has already been submitted. Each transaction ID can only be used once.',
      );
    }

    // Update order with UTR
    order.utrId = cleanUtr;
    order.status = UpiPaymentStatus.UTR_SUBMITTED;
    await order.save();

    return {
      orderId: order.orderId,
      status: 'utr_submitted',
      message:
        'UTR submitted successfully. Your payment is being verified. You will receive your activation code shortly.',
    };
  }

  // ══════════════════════════════
  //  CHECK ORDER STATUS
  // ══════════════════════════════

  async getOrderStatus(orderId: string, userId?: string) {
    const order = await this.paymentModel.findOne({ orderId }).lean();
    if (!order) throw new NotFoundException('Order not found');

    if (userId && order.userId && order.userId.toString() !== userId) {
      throw new BadRequestException('Order does not belong to you');
    }

    return {
      orderId: order.orderId,
      plan: order.planName,
      amount: order.amount,
      status: order.status,
      utrId: order.utrId || null,
      activationCode:
        order.status === UpiPaymentStatus.VERIFIED
          ? order.activationCode
          : null,
      rejectionReason:
        order.status === UpiPaymentStatus.REJECTED
          ? order.rejectionReason
          : null,
      createdAt: (order as any).createdAt,
    };
  }

  // ══════════════════════════════
  //  USER: MY ORDERS
  // ══════════════════════════════

  async getUserOrders(userId: string) {
    const orders = await this.paymentModel
      .find({ userId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .limit(20)
      .lean();

    return orders.map((o) => ({
      orderId: o.orderId,
      plan: o.planName,
      amount: o.amount,
      status: o.status,
      utrId: o.utrId || null,
      activationCode:
        o.status === UpiPaymentStatus.VERIFIED ? o.activationCode : null,
      createdAt: (o as any).createdAt,
    }));
  }

  // ══════════════════════════════
  //  ADMIN: VERIFY PAYMENT
  // ══════════════════════════════

  async adminVerify(paymentId: string) {
    const payment = await this.paymentModel.findById(paymentId);
    if (!payment) throw new NotFoundException('Payment not found');

    if (payment.status === UpiPaymentStatus.VERIFIED) {
      throw new BadRequestException('Already verified');
    }

    // Generate activation code
    const planEnum = payment.plan as PremiumPlan;
    const codes = await this.premiumService.generateCodes(planEnum, 1, {
      note: `UPI Payment | Order: ${payment.orderId} | UTR: ${payment.utrId}`,
      expiresInDays: 30,
    });

    payment.status = UpiPaymentStatus.VERIFIED;
    payment.activationCode = codes[0].code;
    payment.verifiedAt = new Date();
    await payment.save();

    return {
      success: true,
      activationCode: codes[0].code,
      orderId: payment.orderId,
    };
  }

  // ══════════════════════════════
  //  ADMIN: REJECT PAYMENT
  // ══════════════════════════════

  async adminReject(paymentId: string, reason?: string) {
    const payment = await this.paymentModel.findById(paymentId);
    if (!payment) throw new NotFoundException('Payment not found');

    payment.status = UpiPaymentStatus.REJECTED;
    payment.rejectedAt = new Date();
    payment.rejectionReason = reason || 'Rejected by admin';
    await payment.save();

    return { success: true };
  }

  // ══════════════════════════════
  //  ADMIN: LIST PAYMENTS
  // ══════════════════════════════

  async getPayments(filters?: {
    status?: string;
    page?: number;
    limit?: number;
  }) {
    const query: any = {};
    if (filters?.status) query.status = filters.status;
    const page = filters?.page || 1;
    const limit = filters?.limit || 50;

    const [payments, total] = await Promise.all([
      this.paymentModel
        .find(query)
        .sort({ createdAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit)
        .lean(),
      this.paymentModel.countDocuments(query),
    ]);

    return { payments, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  // ══════════════════════════════
  //  ADMIN: STATS
  // ══════════════════════════════

  async getStats() {
    const [total, verified, pending, utrSubmitted, rejected] =
      await Promise.all([
        this.paymentModel.countDocuments(),
        this.paymentModel.countDocuments({ status: UpiPaymentStatus.VERIFIED }),
        this.paymentModel.countDocuments({ status: UpiPaymentStatus.PENDING }),
        this.paymentModel.countDocuments({
          status: UpiPaymentStatus.UTR_SUBMITTED,
        }),
        this.paymentModel.countDocuments({ status: UpiPaymentStatus.REJECTED }),
      ]);

    const totalRevenue = await this.paymentModel.aggregate([
      { $match: { status: UpiPaymentStatus.VERIFIED } },
      { $group: { _id: null, total: { $sum: '$amount' } } },
    ]);

    return {
      total,
      verified,
      pending,
      utrSubmitted,
      rejected,
      totalRevenue: totalRevenue[0]?.total || 0,
    };
  }
}
