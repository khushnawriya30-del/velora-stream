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

interface UserSession {
  plan: string;
  amount: number;
  planName: string;
  state: 'selected_plan' | 'awaiting_utr';
}

@Injectable()
export class TelegramBotService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(TelegramBotService.name);
  private bot: Telegraf | null = null;
  private isRunning = false;

  // In-memory state per Telegram user
  private userSessions = new Map<string, UserSession>();

  // Map planId to PremiumPlan enum
  private readonly planEnumMap: Record<string, string> = {
    '1m': '1month',
    '3m': '3months',
    '6m': '6months',
    '12m': '1year',
  };

  constructor(
    @InjectModel(TelegramPayment.name)
    private paymentModel: Model<TelegramPaymentDocument>,
    @InjectModel(PremiumPlanConfig.name)
    private planModel: Model<PremiumPlanConfigDocument>,
    private readonly settingsService: SettingsService,
    private readonly premiumService: PremiumService,
  ) {}

  // ══════════════════════════════
  //  LIFECYCLE
  // ══════════════════════════════

  async onModuleInit() {
    // Delay bot start to let NestJS fully initialize
    setTimeout(() => this.startBot(), 3000);
  }

  async onModuleDestroy() {
    await this.stopBot();
  }

  async startBot() {
    try {
      const settings = await this.settingsService.getSettings();
      const token = settings?.telegramBotToken;

      if (!token) {
        this.logger.warn('Telegram bot token not configured - skipping start');
        return;
      }

      // Stop existing bot if running
      if (this.bot) {
        try {
          this.bot.stop('restart');
        } catch {}
        this.bot = null;
        this.isRunning = false;
      }

      this.bot = new Telegraf(token);
      this.registerHandlers();

      // Launch with polling, drop old updates
      await this.bot.launch({ dropPendingUpdates: true });
      this.isRunning = true;
      this.logger.log('Telegram bot started successfully');
    } catch (error: any) {
      this.logger.error('Failed to start Telegram bot: ' + error.message);
      this.isRunning = false;
      this.bot = null;
    }
  }

  async stopBot() {
    if (this.bot) {
      try {
        await this.bot.stop('shutdown');
      } catch {}
      this.isRunning = false;
      this.bot = null;
      this.logger.log('Telegram bot stopped');
    }
  }

  async restartBot() {
    try {
      await this.stopBot();
      await new Promise((r) => setTimeout(r, 1000));
      await this.startBot();
    } catch (e: any) {
      this.logger.error('Restart failed: ' + e.message);
    }
  }

  // ══════════════════════════════
  //  HANDLERS
  // ══════════════════════════════

  private registerHandlers() {
    if (!this.bot) return;

    // ── /start ──
    this.bot.start(async (ctx) => {
      try {
        const firstName = ctx.from.first_name || 'User';
        this.userSessions.delete(ctx.from.id.toString());

        await ctx.replyWithHTML(
          `<b>Welcome to Velora Premium, ${this.esc(firstName)}!</b>\n\n` +
            `Get Premium access to unlimited movies, series & more.\n\n` +
            `Tap <b>Purchase Plan</b> to get started.`,
          Markup.keyboard([['Purchase Plan'], ['Exit']])
            .resize()
            .oneTime(false),
        );
      } catch (e: any) {
        this.logger.error('start error: ' + e.message);
      }
    });

    // ── Purchase Plan ──
    this.bot.hears('Purchase Plan', async (ctx) => {
      try {
        await this.showPlans(ctx);
      } catch (e: any) {
        this.logger.error('showPlans: ' + e.message);
      }
    });

    // ── Exit ──
    this.bot.hears('Exit', async (ctx) => {
      try {
        this.userSessions.delete(ctx.from.id.toString());
        await ctx.reply(
          'Thank you for visiting Velora Premium!\n\nType /start anytime to come back.',
          Markup.removeKeyboard(),
        );
      } catch {}
    });

    // ── My Payments ──
    this.bot.hears('My Payments', async (ctx) => {
      try {
        await this.showMyPayments(ctx);
      } catch (e: any) {
        this.logger.error('myPayments: ' + e.message);
      }
    });

    // ── Help ──
    this.bot.hears('Help', async (ctx) => {
      try {
        await ctx.replyWithHTML(
          `<b>How to buy Premium:</b>\n\n` +
            `1. Tap <b>Purchase Plan</b>\n` +
            `2. Select your plan\n` +
            `3. Scan QR code and pay via UPI\n` +
            `4. Tap <b>Done</b> after payment\n` +
            `5. Enter your UTR / Transaction ID\n` +
            `6. Get your activation code\n` +
            `7. Open app → Premium → Activate Code`,
        );
      } catch {}
    });

    // ── Plan selection callback ──
    this.bot.action(/^plan_(.+)$/, async (ctx) => {
      try {
        await ctx.answerCbQuery();
        await this.handlePlanSelection(ctx, ctx.match[1]);
      } catch (e: any) {
        this.logger.error('planSel: ' + e.message);
      }
    });

    // ── Done button (after payment) ──
    this.bot.action('payment_done', async (ctx) => {
      try {
        await ctx.answerCbQuery();
        const tgUserId = ctx.from.id.toString();
        const session = this.userSessions.get(tgUserId);

        if (!session || session.state !== 'selected_plan') {
          await ctx.editMessageText(
            'No active payment session. Type /start to begin again.',
          );
          return;
        }

        // Move to awaiting UTR state
        session.state = 'awaiting_utr';
        this.userSessions.set(tgUserId, session);

        await ctx.editMessageText(
          'Please send your UTR ID / Transaction ID now.\n\n' +
            '(The reference number from your UPI payment)',
        );
      } catch (e: any) {
        this.logger.error('paymentDone: ' + e.message);
      }
    });

    // ── Cancel payment ──
    this.bot.action('cancel_payment', async (ctx) => {
      try {
        await ctx.answerCbQuery('Cancelled');
        this.userSessions.delete(ctx.from.id.toString());
        await ctx.editMessageText(
          'Payment cancelled.\n\nType /start or tap Purchase Plan to try again.',
        );
      } catch {}
    });

    // ── Text handler (UTR submission) ──
    this.bot.on('text', async (ctx) => {
      try {
        const tgUserId = ctx.from.id.toString();
        const session = this.userSessions.get(tgUserId);

        if (session?.state === 'awaiting_utr') {
          await this.handleUtrSubmission(ctx, session);
        }
      } catch (e: any) {
        this.logger.error('text: ' + e.message);
      }
    });

    // Global error handler
    this.bot.catch((err: any) => {
      this.logger.error('Bot error: ' + (err?.message || err));
    });
  }

  // ══════════════════════════════
  //  SHOW PLANS
  // ══════════════════════════════

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
        `${p.name} - ₹${p.price}${p.badge ? ` (${p.badge})` : ''}`,
        `plan_${p.planId}`,
      ),
    ]);

    const planLines = plans.map(
      (p) =>
        `• <b>${p.name}</b> - ₹${p.price}` +
        (p.originalPrice > p.price
          ? ` <s>₹${p.originalPrice}</s> (${p.discountPercent}% off)`
          : '') +
        (p.badge ? ` | ${p.badge}` : ''),
    );

    await ctx.replyWithHTML(
      `<b>Velora Premium Plans</b>\n\n` +
        planLines.join('\n') +
        `\n\n<i>Select a plan:</i>`,
      Markup.inlineKeyboard(planButtons),
    );
  }

  // ══════════════════════════════
  //  PLAN SELECTION → QR + Amount
  // ══════════════════════════════

  private async handlePlanSelection(ctx: any, planId: string) {
    const plan = await this.planModel
      .findOne({ planId, isActive: true })
      .lean();
    if (!plan) {
      await ctx.editMessageText('This plan is no longer available.');
      return;
    }

    const tgUserId = ctx.from.id.toString();
    this.userSessions.set(tgUserId, {
      plan: this.planEnumMap[plan.planId] || plan.planId,
      amount: plan.price,
      planName: plan.name,
      state: 'selected_plan',
    });

    const settings = await this.settingsService.getSettings();

    // Send QR code
    if (settings.paymentQrCodeUrl) {
      try {
        if (settings.paymentQrCodeUrl.startsWith('data:')) {
          const base64Data = settings.paymentQrCodeUrl.split(',')[1];
          const buffer = Buffer.from(base64Data, 'base64');
          await ctx.replyWithPhoto({ source: buffer });
        } else {
          await ctx.replyWithPhoto(settings.paymentQrCodeUrl);
        }
      } catch (e: any) {
        this.logger.warn('Failed to send QR: ' + e.message);
      }
    }

    const upiId = settings.paymentUpiId || 'N/A';
    const instructions =
      settings.paymentInstructions ||
      'Please scan the QR code and complete the payment.';

    // Show payment details + Done/Cancel buttons
    await ctx.replyWithHTML(
      `<b>${this.esc(plan.name)} - ₹${plan.price}</b>\n\n` +
        `<b>UPI ID:</b> <code>${this.esc(upiId)}</code>\n\n` +
        `${this.esc(instructions)}\n\n` +
        `After payment, click <b>Done</b> below.`,
      Markup.inlineKeyboard([
        [Markup.button.callback('✅ Done', 'payment_done')],
        [Markup.button.callback('❌ Cancel', 'cancel_payment')],
      ]),
    );
  }

  // ══════════════════════════════
  //  UTR SUBMISSION & VERIFY
  // ══════════════════════════════

  private async handleUtrSubmission(ctx: any, session: UserSession) {
    const tgUserId = ctx.from.id.toString();
    const utrId = ctx.message.text.trim().toUpperCase();

    // Validate UTR format
    if (!/^[A-Z0-9]{6,30}$/.test(utrId)) {
      await ctx.reply(
        'Invalid UTR format. Please enter a valid UTR / Transaction ID (6-30 alphanumeric characters).',
      );
      return;
    }

    // Check duplicate UTR
    const existing = await this.paymentModel.findOne({ utrId }).lean();
    if (existing) {
      await ctx.replyWithHTML(
        `<b>This UTR ID has already been used.</b>\n\n` +
          `Each UTR can only be used once. Please check and try again.`,
      );
      return;
    }

    const processingMsg = await ctx.reply('Verifying your payment...');

    try {
      // Generate activation code
      const planEnum = session.plan as PremiumPlan;
      const codes = await this.premiumService.generateCodes(planEnum, 1, {
        note: `Telegram | UTR: ${utrId} | User: ${tgUserId}`,
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

      // Send success + code
      await ctx.replyWithHTML(
        `<b>Payment Verified Successfully!</b>\n\n` +
          `<b>Plan:</b> ${this.esc(session.planName)}\n` +
          `<b>Amount:</b> ₹${session.amount}\n` +
          `<b>UTR:</b> <code>${utrId}</code>\n\n` +
          `<b>Your Premium Activation Code:</b>\n\n` +
          `<code>${activationCode}</code>\n\n` +
          `<i>Use this code in the app to activate Premium:\nOpen App → Premium → Activate Code</i>`,
        Markup.keyboard([['Purchase Plan'], ['My Payments', 'Help']])
          .resize()
          .oneTime(false),
      );
    } catch (error: any) {
      this.logger.error(
        `UTR verify failed for ${utrId}: ${error.message}`,
      );

      try {
        await ctx.deleteMessage(processingMsg.message_id);
      } catch {}

      await ctx.reply(
        'Verification failed. Please check your UTR ID and try again.\nIf the issue persists, contact support.',
      );
    }
  }

  // ══════════════════════════════
  //  MY PAYMENTS
  // ══════════════════════════════

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

    const emoji: Record<string, string> = {
      pending: '⏳',
      verified: '✅',
      rejected: '❌',
      expired: '⌛',
    };

    const lines = payments.map((p, i) => {
      const e = emoji[p.status] || '❓';
      const date = new Date((p as any).createdAt).toLocaleDateString('en-IN');
      return (
        `${i + 1}. ${e} <b>${p.plan}</b> - ₹${p.amount}\n` +
        `   UTR: <code>${p.utrId}</code>\n` +
        (p.activationCode
          ? `   Code: <code>${p.activationCode}</code>\n`
          : '') +
        `   ${date} | ${p.status}`
      );
    });

    await ctx.replyWithHTML(
      `<b>Your Payments (last 10)</b>\n\n${lines.join('\n\n')}`,
    );
  }

  // ══════════════════════════════
  //  ADMIN API METHODS
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

    return {
      payments,
      total,
      page,
      limit,
      totalPages: Math.ceil(total / limit),
    };
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

    const planEnum = payment.plan as PremiumPlan;
    const codes = await this.premiumService.generateCodes(planEnum, 1, {
      note: `Manual verify | UTR: ${payment.utrId} | TG: ${payment.telegramUserId}`,
      expiresInDays: 30,
    });

    payment.status = PaymentStatus.VERIFIED;
    payment.activationCode = codes[0].code;
    payment.verifiedAt = new Date();
    await payment.save();

    // Notify user via bot
    if (this.bot && this.isRunning) {
      try {
        await this.bot.telegram.sendMessage(
          payment.telegramUserId,
          `<b>Payment Approved!</b>\n\n` +
            `Your Premium Activation Code:\n\n` +
            `<code>${codes[0].code}</code>\n\n` +
            `<i>Enter this code in the app → Premium → Activate Code</i>`,
          { parse_mode: 'HTML' },
        );
      } catch {
        this.logger.warn(
          `Could not send code to TG user ${payment.telegramUserId}`,
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

  private esc(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }
}
