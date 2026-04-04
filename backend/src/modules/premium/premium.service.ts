import { Injectable, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { User, UserDocument } from '../../schemas/user.schema';
import {
  ActivationCode,
  ActivationCodeDocument,
  PremiumPlan,
} from '../../schemas/activation-code.schema';
import * as crypto from 'crypto';

@Injectable()
export class PremiumService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    @InjectModel(ActivationCode.name)
    private codeModel: Model<ActivationCodeDocument>,
  ) {}

  // ── Code Generation ──

  private generateCode(): string {
    // Format: VLRA-XXXX-XXXX (12 chars + dashes)
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // No I,O,0,1 to avoid confusion
    let code = 'VLRA-';
    for (let i = 0; i < 8; i++) {
      if (i === 4) code += '-';
      code += chars[crypto.randomInt(chars.length)];
    }
    return code;
  }

  private getDurationDays(plan: PremiumPlan): number {
    switch (plan) {
      case PremiumPlan.ONE_MONTH:
        return 30;
      case PremiumPlan.THREE_MONTHS:
        return 90;
      case PremiumPlan.SIX_MONTHS:
        return 180;
      case PremiumPlan.ONE_YEAR:
        return 365;
    }
  }

  async generateCodes(
    plan: PremiumPlan,
    count: number,
    options?: { batchId?: string; note?: string; expiresInDays?: number },
  ) {
    const codes = [];
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + (options?.expiresInDays || 30));

    for (let i = 0; i < count; i++) {
      let code: string;
      let attempts = 0;
      // Ensure uniqueness
      do {
        code = this.generateCode();
        attempts++;
      } while (
        (await this.codeModel.findOne({ code })) !== null &&
        attempts < 10
      );

      codes.push({
        code,
        plan,
        durationDays: this.getDurationDays(plan),
        expiresAt,
        batchId: options?.batchId || `batch-${Date.now()}`,
        note: options?.note || '',
      });
    }

    const result = await this.codeModel.insertMany(codes);
    return result.map((c) => ({
      code: c.code,
      plan: c.plan,
      durationDays: c.durationDays,
      expiresAt: c.expiresAt,
    }));
  }

  // ── Code Activation (User-facing) ──

  async activateCode(userId: string, code: string) {
    const cleanCode = code.trim().toUpperCase();

    const activationCode = await this.codeModel.findOne({ code: cleanCode });
    if (!activationCode) {
      throw new BadRequestException('Invalid activation code');
    }
    if (activationCode.isRedeemed) {
      throw new BadRequestException(
        'This code has already been redeemed',
      );
    }
    if (activationCode.isRevoked) {
      throw new BadRequestException('This code has been revoked');
    }
    if (activationCode.expiresAt && activationCode.expiresAt < new Date()) {
      throw new BadRequestException(
        'This code has expired. Please get a new one.',
      );
    }

    // Calculate new expiry
    const user = await this.userModel.findById(userId);
    if (!user) throw new BadRequestException('User not found');

    // If already premium, extend from current expiry; otherwise from now
    const baseDate =
      user.isPremium && user.premiumExpiresAt > new Date()
        ? user.premiumExpiresAt
        : new Date();
    const newExpiry = new Date(baseDate);
    newExpiry.setDate(newExpiry.getDate() + activationCode.durationDays);

    // Mark code as redeemed
    activationCode.isRedeemed = true;
    activationCode.redeemedBy = new Types.ObjectId(userId);
    activationCode.redeemedAt = new Date();
    await activationCode.save();

    // Update user premium status
    await this.userModel.findByIdAndUpdate(userId, {
      isPremium: true,
      premiumPlan: activationCode.plan,
      premiumExpiresAt: newExpiry,
      premiumActivatedAt: new Date(),
      activationCode: cleanCode,
      maxDevices: 2,
    });

    return {
      success: true,
      plan: activationCode.plan,
      expiresAt: newExpiry,
      durationDays: activationCode.durationDays,
    };
  }

  // ── Premium Status Check ──

  async getPremiumStatus(userId: string) {
    const user = await this.userModel.findById(userId).lean();
    if (!user) throw new BadRequestException('User not found');

    const isActive =
      user.isPremium && user.premiumExpiresAt && user.premiumExpiresAt > new Date();
    const daysRemaining = isActive
      ? Math.ceil(
          (user.premiumExpiresAt.getTime() - Date.now()) / (1000 * 60 * 60 * 24),
        )
      : 0;

    return {
      isPremium: !!isActive,
      plan: isActive ? user.premiumPlan : null,
      expiresAt: isActive ? user.premiumExpiresAt : null,
      daysRemaining,
      activatedAt: user.premiumActivatedAt || null,
    };
  }

  // ── Admin: List Codes ──

  async listCodes(filters?: {
    plan?: string;
    isRedeemed?: boolean;
    batchId?: string;
    page?: number;
    limit?: number;
  }) {
    const query: any = {};
    if (filters?.plan) query.plan = filters.plan;
    if (filters?.isRedeemed !== undefined) query.isRedeemed = filters.isRedeemed;
    if (filters?.batchId) query.batchId = filters.batchId;

    const page = filters?.page || 1;
    const limit = filters?.limit || 50;

    const [codes, total] = await Promise.all([
      this.codeModel
        .find(query)
        .sort({ createdAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit)
        .populate('redeemedBy', 'name email')
        .lean(),
      this.codeModel.countDocuments(query),
    ]);

    return { codes, total, page, limit, totalPages: Math.ceil(total / limit) };
  }

  // ── Admin: Revoke Code ──

  async revokeCode(codeId: string, reason?: string) {
    const code = await this.codeModel.findById(codeId);
    if (!code) throw new BadRequestException('Code not found');

    code.isRevoked = true;
    code.revokedReason = reason || 'Revoked by admin';
    await code.save();

    // If code was redeemed, also remove premium from user
    if (code.isRedeemed && code.redeemedBy) {
      await this.userModel.findByIdAndUpdate(code.redeemedBy, {
        isPremium: false,
        premiumPlan: null,
        premiumExpiresAt: null,
      });
    }

    return { success: true };
  }

  // ── Admin: List Premium Users ──

  async listPremiumUsers(page = 1, limit = 50) {
    const query = { isPremium: true };
    const [users, total] = await Promise.all([
      this.userModel
        .find(query)
        .select('name email isPremium premiumPlan premiumExpiresAt premiumActivatedAt activationCode')
        .sort({ premiumActivatedAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit)
        .lean(),
      this.userModel.countDocuments(query),
    ]);

    return { users, total, page, limit };
  }

  // ── Admin: Dashboard Stats ──

  async getStats() {
    const [
      totalCodes,
      redeemedCodes,
      activePremiumUsers,
      expiringSoon,
    ] = await Promise.all([
      this.codeModel.countDocuments(),
      this.codeModel.countDocuments({ isRedeemed: true }),
      this.userModel.countDocuments({
        isPremium: true,
        premiumExpiresAt: { $gt: new Date() },
      }),
      this.userModel.countDocuments({
        isPremium: true,
        premiumExpiresAt: {
          $gt: new Date(),
          $lt: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000), // 3 days
        },
      }),
    ]);

    // Plan breakdown
    const planBreakdown = await this.userModel.aggregate([
      { $match: { isPremium: true, premiumExpiresAt: { $gt: new Date() } } },
      { $group: { _id: '$premiumPlan', count: { $sum: 1 } } },
    ]);

    return {
      totalCodes,
      redeemedCodes,
      availableCodes: totalCodes - redeemedCodes,
      activePremiumUsers,
      expiringSoon,
      planBreakdown: planBreakdown.reduce(
        (acc, p) => ({ ...acc, [p._id]: p.count }),
        {},
      ),
    };
  }

  // ── Cron: Expire Subscriptions ──

  async expireSubscriptions() {
    const result = await this.userModel.updateMany(
      {
        isPremium: true,
        premiumExpiresAt: { $lt: new Date() },
      },
      {
        isPremium: false,
      },
    );
    return { expiredCount: result.modifiedCount };
  }
}
