import { Injectable, Logger, BadRequestException, ConflictException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Referral, ReferralDocument } from '../../schemas/referral.schema';
import { User, UserDocument } from '../../schemas/user.schema';
import { WalletService } from '../wallet/wallet.service';
import { randomBytes } from 'crypto';

@Injectable()
export class ReferralService {
  private readonly logger = new Logger(ReferralService.name);

  constructor(
    @InjectModel(Referral.name) private referralModel: Model<ReferralDocument>,
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    private readonly walletService: WalletService,
  ) {}

  /** Get or generate referral code for a user */
  async getReferralCode(userId: string): Promise<string> {
    const user = await this.userModel.findById(userId);
    if (!user) throw new BadRequestException('User not found');

    if (!user.referralCode) {
      user.referralCode = this.generateCode();
      await user.save();
    }
    return user.referralCode;
  }

  /** Apply referral: new user was referred by someone */
  async applyReferral(newUserId: string, referralCode: string): Promise<void> {
    // Find the referrer
    const referrer = await this.userModel.findOne({ referralCode });
    if (!referrer) throw new BadRequestException('Invalid referral code');

    // Prevent self-referral
    if (referrer._id.toString() === newUserId) {
      throw new BadRequestException('Cannot refer yourself');
    }

    // Check if this new user was already referred
    const existing = await this.referralModel.findOne({
      newUserId: new Types.ObjectId(newUserId),
    });
    if (existing) throw new ConflictException('Referral already applied');

    // Check if referrer already referred this user
    const duplicate = await this.referralModel.findOne({
      referrerId: referrer._id,
      newUserId: new Types.ObjectId(newUserId),
    });
    if (duplicate) throw new ConflictException('Duplicate referral');

    // Create referral record
    await this.referralModel.create({
      referrerId: referrer._id,
      newUserId: new Types.ObjectId(newUserId),
      amount: 1,
      status: 'success',
    });

    // Mark referredBy on the new user
    await this.userModel.findByIdAndUpdate(newUserId, {
      referredBy: referrer._id,
    });

    // Add ₹1 to referrer's wallet
    await this.walletService.addEarnings(referrer._id.toString(), 1);

    this.logger.log(`Referral applied: ${referrer._id} referred ${newUserId}, ₹1 added`);
  }

  /** Get referral stats for a user */
  async getReferralStats(userId: string) {
    const referralCode = await this.getReferralCode(userId);
    const referrals = await this.referralModel
      .find({ referrerId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .lean();

    return {
      referralCode,
      totalInvited: referrals.length,
      totalEarned: referrals.reduce((sum, r) => sum + r.amount, 0),
      referrals: referrals.map((r) => ({
        id: r._id,
        amount: r.amount,
        status: r.status,
        createdAt: (r as any).createdAt,
      })),
    };
  }

  /** Get earnings history */
  async getEarningsHistory(userId: string) {
    const referrals = await this.referralModel
      .find({ referrerId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .lean();

    return referrals.map((r) => ({
      id: r._id,
      type: 'referral',
      amount: r.amount,
      description: 'Referral bonus',
      createdAt: (r as any).createdAt,
    }));
  }

  private generateCode(): string {
    return 'VEL' + randomBytes(4).toString('hex').toUpperCase();
  }
}
