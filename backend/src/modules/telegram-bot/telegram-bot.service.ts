import {
  Injectable,
  Logger,
  OnModuleInit,
  OnModuleDestroy,
  BadRequestException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Telegraf, Markup } from 'telegraf';
import {
  TelegramPayment,
  TelegramPaymentDocument,
  PaymentStatus,
} from '../../schemas/telegram-payment.schema';
import {
  PremiumPlanConfig,
  PremiumPlanConfigDocument,
} from '../../schemas/premium-plan.schema';
import { PremiumPlan } from '../../schemas/activation-code.schema';
import { SettingsService } from '../settings/settings.service';
import { PremiumService } from '../premium/premium.service';

@Injectable()
export class TelegramBotService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(TelegramBotService.name);
  private bot: Telegraf | null = null;
  private isRunning = false;

  // In-memory state for user sessions (telegramUserId → selected plan)
  private userSessions = new Map<
    string,
    { plan: string; amount: number; planName: string; awaitingUtr: boolean }
  >();

  constructor(
    @InjectModel(TelegramPayment.name)
    private paymentModel: Model<TelegramPaymentDocument>,
    @InjectModel(PremiumPlanConfig.name)
    private planModel: Model<PremiumPlanConfigDocument>,
    private readonly settingsService: SettingsService,
    private readonly premiumService: PremiumService,
  ) {}

  async onModuleInit() {
    await this.startBot();
  }

  async onModuleDestroy() {
    await this.stopBot();
  }

  // ══════════════════════════════════════════
  //  BOT LIFECYCLE
  // ══════════════════════════════════════════

  async startBot() {
    try {
      const settings = await this.settingsService.getSettings();
      const token = settings.telegramBotToken;

      if (!token) {
        this.logger.warn(
          'Telegram bot token not configured. Set it in Admin → Settings.',
        );
        return;
      }

      this.bot = new Telegraf(token);
      this.registerHandlers();

      // Use polling (not webhook) for simplicity
      await this.bot.launch({ dropPendingUpdates: true });
      this.isRunning = true;
      this.logger.log('Telegram bot started successfully');
    } catch (error) {
      this.logger.error('Failed to start Telegram bot:', error.message);
    }
  }

  async stopBot() {
    if (this.bot && this.isRunning) {
      this.bot.stop('NestJS shutdown');
      this.isRunning = false;
      this.logger.log('Telegram bot stopped');
    }
  }

  async restartBot() {
    await this.stopBot();
    await this.startBot();
  }

  // ══════════════════════════════════════════
  //  BOT HANDLERS
  // ══════════════════════════════════════════

  private registerHandlers() {
    if (!this.bot) return;

    // /start command
    this.bot.start(async (ctx) => {
      const firstName = ctx.from.first_name || 'there';
      await ctx.replyWithHTML(
        `<b>👋 Welcome to VELORA Premium, ${this.escapeHtml(firstName)}!</b>\n\n` +
          `Purchase Premium subscription via UPI payment.\n\n` +
          `Tap <b>"Buy Premium"</b> below to get started.`,
        Markup.keyboard([['🛒 Buy Premium'], ['📋 My Payments', '❓ Help']])
          .resize()
          .oneTime(false),
      );
    });

    // Buy Premium button
    this.bot.hears('🛒 Buy Premium', async (ctx) => {
      await this.showPlans(ctx);
    });

    // My Payments button
    this.bot.hears('📋 My Payments', async (ctx) => {
      await this.showMyPayments(ctx);
    });

    // Help button
    this.bot.hears('❓ Help', async (ctx) => {
      await ctx.replyWithHTML(
        `<b>ℹ️ How to buy Premium:</b>\n\n` +
          `1. Tap <b>"Buy Premium"</b>\n` +
          `2. Select your plan\n` +
          `3. Pay using the QR code shown\n` +
          `4. Enter your UTR/Transaction ID\n` +
          `5. Get your activation code\n` +
          `6. Enter code in the app → Premium → Activate Code\n\n` +
          `<b>Need help?</b> Contact support.`,
      );
    });

    // Plan selection callbacks
    this.bot.action(/^plan_(.+)$/, async (ctx) => {
      await ctx.answerCbQuery();
      const planId = ctx.match[1];
      await this.handlePlanSelection(ctx, planId);
    });

    // Cancel payment callback
    this.bot.action('cancel_payment', async (ctx) => {
      await ctx.answerCbQuery('Cancelled');
      const tgUserId = ctx.from.id.toString();
      this.userSessions.delete(tgUserId);
      await ctx.editMessageText('❌ Payment cancelled. Tap "Buy Premium" to start again.');
    });

    // Handle text input (UTR ID)
    this.bot.on('text', async (ctx) => {
      const tgUserId = ctx.from.id.toString();
      const session = this.userSessions.get(tgUserId);

      if (session?.awaitingUtr) {
        await this.handleUtrSubmission(ctx, session);
      }
    });
  }

  // ══════════════════════════════════════════
  //  PLAN SELECTION
  // ══════════════════════════════════════════

  private async showPlans(ctx: any) {
    const plans = await this.planModel
      .find({ isActive: true })
      .sort({ order: 1 })
      .lean();

    if (plans.length === 0) {
      await ctx.reply('No plans available right now. Please try again later.');
      return;
    }

    const planButtons = plans.map((p) => [
      Markup.button.callback(
        `${p.name} — ₹${p.price} ${p.badge ? `(${p.badge})` : ''}`,
        `plan_${p.planId}`,
      ),
    ]);

    await ctx.replyWithHTML(
      `<b>🔱 VELORA Premium Plans</b>\n\n` +
        plans
          .map(
            (p) =>
              `• <b>${p.name}</b> — ₹${p.price} ` +
              (p.originalPrice > p.price
                ? `<s>₹${p.originalPrice}</s> (${p.discountPercent}% off)`
                : '') +
              (p.badge ? ` 🏷 ${p.badge}` : ''),
          )
          .join('\n') +
        `\n\n<i>Select a plan below:</i>`,
      Markup.inlineKeyboard(planButtons),
    );
  }

  private async handlePlanSelection(ctx: any, planId: string) {
    const plan = await this.planModel.findOne({ planId, isActive: true }).lean();
    if (!plan) {
      await ctx.editMessageText('This plan is no longer available.');
      return;
    }

    const tgUserId = ctx.from.id.toString();

    // Map planId to PremiumPlan enum value
    const planEnumMap: Record<string, string> = {
      '1m': '1month',
      '3m': '3months',
      '6m': '6months',
      '12m': '1year',
    };

    this.userSessions.set(tgUserId, {
      plan: planEnumMap[plan.planId] || plan.planId,
      amount: plan.price,
      planName: plan.name,
      awaitingUtr: true,
    });

    const settings = await this.settingsService.getSettings();

    // Send QR code if available
    if (settings.paymentQrCodeUrl) {
      try {
        await ctx.replyWithPhoto(settings.paymentQrCodeUrl);
      } catch {
        this.logger.warn('Failed to send QR code image');
      }
    }

    const upiId = settings.paymentUpiId || 'N/A';
    const instructions =
      settings.paymentInstructions ||
      'Please complete the payment using the QR code above or the UPI ID.';

    await ctx.replyWithHTML(
      `<b>💳 Payment for ${this.escapeHtml(plan.name)}</b>\n\n` +
        `<b>Amount:</b> ₹${plan.price}\n` +
        `<b>UPI ID:</b> <code>${this.escapeHtml(upiId)}</code>\n\n` +
        `${this.escapeHtml(instructions)}\n\n` +
        `After payment, <b>enter your UTR ID / Transaction ID</b> below to verify your payment.`,
      Markup.inlineKeyboard([
        [Markup.button.callback('❌ Cancel Payment', 'cancel_payment')],
      ]),
    );
  }

  // ══════════════════════════════════════════
  //  UTR SUBMISSION & VERIFICATION
  // ══════════════════════════════════════════

  private async handleUtrSubmission(
    ctx: any,
    session: { plan: string; amount: number; planName: string },
  ) {
    const tgUserId = ctx.from.id.toString();
    const utrId = ctx.message.text.trim().toUpperCase();

    // Basic UTR validation (must be alphanumeric, 6-30 chars)
    if (!/^[A-Z0-9]{6,30}$/.test(utrId)) {
      await ctx.replyWithHTML(
        `⚠️ Invalid UTR format. Please enter a valid UTR/Transaction ID (6-30 alphanumeric characters).`,
      );
      return;
    }

    // Check if UTR already used
    const existingPayment = await this.paymentModel.findOne({ utrId }).lean();
    if (existingPayment) {
      await ctx.replyWithHTML(
        `❌ <b>This UTR ID has already been used.</b>\n\n` +
          `Each UTR can only be used once. Please check your UTR ID and try again.`,
      );
      return;
    }

    // Send processing message
    const processingMsg = await ctx.replyWithHTML('⏳ Verifying your payment...');

    try {
      // Generate activation code using existing PremiumService
      const planEnum = session.plan as PremiumPlan;
      const codes = await this.premiumService.generateCodes(planEnum, 1, {
        note: `Telegram payment | UTR: ${utrId} | User: ${tgUserId}`,
        expiresInDays: 30,
      });

      const activationCode = codes[0].code;

      // Save payment record
      await this.paymentModel.create({
        telegramUserId: tgUserId,
        telegramUsername: ctx.from.username || '',
        telegramFirstName: ctx.from.first_name || '',
        plan: session.plan,
        amount: session.amount,
        utrId,
        status: PaymentStatus.VERIFIED,
        activationCode,
        verifiedAt: new Date(),
      });

      // Clear session
      this.userSessions.delete(tgUserId);

      // Delete processing message
      try {
        await ctx.deleteMessage(processingMsg.message_id);
      } catch {}

      // Send success message with code
      await ctx.replyWithHTML(
        `✅ <b>Payment Verified Successfully!</b>\n\n` +
          `<b>Plan:</b> ${this.escapeHtml(session.planName)}\n` +
          `<b>Amount:</b> ₹${session.amount}\n` +
          `<b>UTR:</b> <code>${utrId}</code>\n\n` +
          `🔑 <b>Your Premium Activation Code:</b>\n\n` +
          `<code>${activationCode}</code>\n\n` +
          `<i>Copy the code and enter it in the app:\nPremium → Activate Code</i>`,
        Markup.inlineKeyboard([
          [
            Markup.button.url(
              '📱 Open VELORA App',
              'https://play.google.com/store/apps/details?id=com.cinevault.app',
            ),
          ],
        ]),
      );
    } catch (error) {
      this.logger.error(`Payment verification failed for UTR ${utrId}:`, error);

      // Delete processing message
      try {
        await ctx.deleteMessage(processingMsg.message_id);
      } catch {}

      await ctx.replyWithHTML(
        `❌ <b>Payment verification failed.</b>\n\n` +
          `Please check your UTR ID and try again.\n` +
          `If the issue persists, contact support.`,
      );
    }
  }

  // ══════════════════════════════════════════
  //  MY PAYMENTS
  // ══════════════════════════════════════════

  private async showMyPayments(ctx: any) {
    const tgUserId = ctx.from.id.toString();
    const payments = await this.paymentModel
      .find({ telegramUserId: tgUserId })
      .sort({ createdAt: -1 })
      .limit(10)
      .lean();

    if (payments.length === 0) {
      await ctx.reply('You have no payment history yet.');
      return;
    }

    const statusEmoji: Record<string, string> = {
      pending: '⏳',
      verified: '✅',
      rejected: '❌',
      expired: '⌛',
    };

    const lines = payments.map((p, i) => {
      const emoji = statusEmoji[p.status] || '❓';
      const date = new Date(p.createdAt).toLocaleDateString('en-IN');
      return (
        `${i + 1}. ${emoji} <b>${p.plan}</b> — ₹${p.amount}\n` +
        `   UTR: <code>${p.utrId}</code>\n` +
        (p.activationCode
          ? `   Code: <code>${p.activationCode}</code>\n`
          : '') +
        `   Date: ${date} | Status: ${p.status}`
      );
    });

    await ctx.replyWithHTML(
      `<b>📋 Your Payments (last 10)</b>\n\n${lines.join('\n\n')}`,
    );
  }

  // ══════════════════════════════════════════
  //  ADMIN API METHODS (called by controller)
  // ══════════════════════════════════════════

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

  async getPaymentStats() {
    const [total, verified, pending, rejected] = await Promise.all([
      this.paymentModel.countDocuments(),
      this.paymentModel.countDocuments({ status: PaymentStatus.VERIFIED }),
      this.paymentModel.countDocuments({ status: PaymentStatus.PENDING }),
      this.paymentModel.countDocuments({ status: PaymentStatus.REJECTED }),
    ]);

    const totalRevenue = await this.paymentModel.aggregate([
      { $match: { status: PaymentStatus.VERIFIED } },
      { $group: { _id: null, total: { $sum: '$amount' } } },
    ]);

    return {
      total,
      verified,
      pending,
      rejected,
      totalRevenue: totalRevenue[0]?.total || 0,
    };
  }

  async rejectPayment(paymentId: string, reason?: string) {
    const payment = await this.paymentModel.findById(paymentId);
    if (!payment) throw new BadRequestException('Payment not found');

    payment.status = PaymentStatus.REJECTED;
    payment.rejectedAt = new Date();
    payment.rejectionReason = reason || 'Rejected by admin';
    await payment.save();

    return { success: true };
  }

  async manualVerifyPayment(paymentId: string) {
    const payment = await this.paymentModel.findById(paymentId);
    if (!payment) throw new BadRequestException('Payment not found');
    if (payment.status === PaymentStatus.VERIFIED) {
      throw new BadRequestException('Payment already verified');
    }

    // Generate code
    const planEnum = payment.plan as PremiumPlan;
    const codes = await this.premiumService.generateCodes(planEnum, 1, {
      note: `Manual verify | UTR: ${payment.utrId} | TG: ${payment.telegramUserId}`,
      expiresInDays: 30,
    });

    payment.status = PaymentStatus.VERIFIED;
    payment.activationCode = codes[0].code;
    payment.verifiedAt = new Date();
    await payment.save();

    // Try to send code to user via bot
    if (this.bot && this.isRunning) {
      try {
        await this.bot.telegram.sendMessage(
          payment.telegramUserId,
          `✅ <b>Payment Manually Approved!</b>\n\n` +
            `Your Premium Activation Code:\n\n` +
            `<code>${codes[0].code}</code>\n\n` +
            `<i>Enter this code in the app → Premium → Activate Code</i>`,
          { parse_mode: 'HTML' },
        );
      } catch {
        this.logger.warn(
          `Could not send code to Telegram user ${payment.telegramUserId}`,
        );
      }
    }

    return { success: true, activationCode: codes[0].code };
  }

  getBotStatus() {
    return {
      isRunning: this.isRunning,
      botUsername: this.bot?.botInfo?.username || null,
    };
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }
}
