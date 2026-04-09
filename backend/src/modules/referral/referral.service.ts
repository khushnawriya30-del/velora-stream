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

  // ── Admin Methods ──

  /** Admin dashboard: all users with referral data, paginated */
  async getAdminDashboard(page: number, limit: number, search?: string) {
    const skip = (page - 1) * limit;

    // Build filter for users who have a referral code (i.e. actively referring)
    const userFilter: any = { referralCode: { $exists: true, $ne: null } };
    if (search) {
      userFilter.$or = [
        { name: { $regex: search, $options: 'i' } },
        { email: { $regex: search, $options: 'i' } },
        { referralCode: { $regex: search, $options: 'i' } },
      ];
    }

    const [users, total] = await Promise.all([
      this.userModel
        .find(userFilter)
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .select('name email referralCode createdAt')
        .lean(),
      this.userModel.countDocuments(userFilter),
    ]);

    // Get referral counts per user
    const userIds = users.map((u) => u._id);
    const referralCounts = await this.referralModel.aggregate([
      { $match: { referrerId: { $in: userIds } } },
      { $group: { _id: '$referrerId', count: { $sum: 1 }, totalEarned: { $sum: '$amount' } } },
    ]);
    const countMap = new Map(referralCounts.map((r) => [r._id.toString(), r]));

    // Get overall stats
    const [totalReferrals, totalEarnings, totalUsers] = await Promise.all([
      this.referralModel.countDocuments(),
      this.referralModel.aggregate([{ $group: { _id: null, total: { $sum: '$amount' } } }]),
      this.userModel.countDocuments({ referralCode: { $exists: true, $ne: null } }),
    ]);

    const items = users.map((u) => {
      const stats = countMap.get(u._id.toString());
      return {
        _id: u._id,
        name: u.name,
        email: u.email,
        referralCode: u.referralCode,
        totalReferrals: stats?.count || 0,
        totalEarned: stats?.totalEarned || 0,
        joinedAt: (u as any).createdAt,
      };
    });

    return {
      items,
      total,
      page,
      pages: Math.ceil(total / limit),
      overview: {
        totalReferrals,
        totalEarnings: totalEarnings[0]?.total || 0,
        totalReferrers: totalUsers,
      },
    };
  }

  /** Admin: detailed view of a specific user's referrals */
  async getAdminUserReferrals(userId: string) {
    const user = await this.userModel
      .findById(userId)
      .select('name email referralCode createdAt')
      .lean();
    if (!user) throw new BadRequestException('User not found');

    const referrals = await this.referralModel
      .find({ referrerId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .lean();

    // Populate referred users
    const referredUserIds = referrals.map((r) => r.newUserId);
    const referredUsers = await this.userModel
      .find({ _id: { $in: referredUserIds } })
      .select('name email createdAt deviceInfo')
      .lean();
    const userMap = new Map(referredUsers.map((u) => [u._id.toString(), u]));

    return {
      user: {
        _id: user._id,
        name: user.name,
        email: user.email,
        referralCode: user.referralCode,
        joinedAt: (user as any).createdAt,
      },
      totalReferrals: referrals.length,
      totalEarned: referrals.reduce((sum, r) => sum + r.amount, 0),
      referrals: referrals.map((r) => {
        const referred = userMap.get(r.newUserId.toString());
        return {
          id: r._id,
          referredUser: referred
            ? { name: referred.name, email: referred.email, device: referred.deviceInfo }
            : null,
          amount: r.amount,
          status: r.status,
          date: (r as any).createdAt,
        };
      }),
    };
  }
}
