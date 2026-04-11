import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Withdrawal, WithdrawalDocument, WithdrawalStatus } from '../../schemas/withdrawal.schema';
import { WalletService } from '../wallet/wallet.service';
import { User, UserDocument } from '../../schemas/user.schema';

interface BankDetails {
  bankName?: string;
  accountNumber?: string;
  ifscCode?: string;
  accountHolderName?: string;
  phoneNumber?: string;
  email?: string;
}

@Injectable()
export class WithdrawalService {
  private readonly logger = new Logger(WithdrawalService.name);

  constructor(
    @InjectModel(Withdrawal.name) private withdrawalModel: Model<WithdrawalDocument>,
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    private readonly walletService: WalletService,
  ) {}

  /** Request a withdrawal — uses saved bank details from wallet */
  async requestWithdrawal(userId: string, amount: number, upiId: string, bankDetails?: BankDetails) {
    if (amount < 100) {
      throw new BadRequestException('Minimum withdrawal is ₹100');
    }

    const balance = await this.walletService.getBalance(userId);
    if (balance.balance < amount) {
      throw new BadRequestException(
        `Insufficient balance. You have ₹${balance.balance}, need ₹${amount}`,
      );
    }

    // Check for pending withdrawal
    const pending = await this.withdrawalModel.findOne({
      userId: new Types.ObjectId(userId),
      status: 'pending',
    });
    if (pending) {
      throw new BadRequestException('You already have a pending withdrawal request');
    }

    // Auto-fetch saved bank details from wallet if not provided
    let finalBank = bankDetails || {} as BankDetails;
    if (!finalBank.bankName) {
      const saved = await this.walletService.getBankDetails(userId);
      if (saved.hasBankDetails) {
        finalBank = {
          bankName: saved.bankName,
          accountNumber: saved.accountNumber,
          ifscCode: saved.ifscCode,
          accountHolderName: saved.accountHolderName,
          phoneNumber: saved.phoneNumber,
          email: saved.email,
        };
      }
    }

    if (!finalBank.bankName || !finalBank.accountNumber) {
      throw new BadRequestException('Please save your bank details first before withdrawing');
    }

    // Deduct balance
    await this.walletService.deductBalance(userId, amount);

    // Create withdrawal request with bank details
    const withdrawal = await this.withdrawalModel.create({
      userId: new Types.ObjectId(userId),
      amount,
      upiId: upiId || '',
      bankName: finalBank.bankName || '',
      accountNumber: finalBank.accountNumber || '',
      ifscCode: finalBank.ifscCode || '',
      accountHolderName: finalBank.accountHolderName || '',
      phoneNumber: finalBank.phoneNumber || '',
      email: finalBank.email || '',
      status: 'pending',
    });

    this.logger.log(`Withdrawal request ₹${amount} by user ${userId}`);
    return {
      id: withdrawal._id,
      amount: withdrawal.amount,
      upiId: withdrawal.upiId,
      bankName: withdrawal.bankName,
      accountNumber: withdrawal.accountNumber,
      ifscCode: withdrawal.ifscCode,
      accountHolderName: withdrawal.accountHolderName,
      phoneNumber: withdrawal.phoneNumber,
      email: withdrawal.email,
      status: withdrawal.status,
      createdAt: (withdrawal as any).createdAt,
    };
  }

  /** Get withdrawal history */
  async getHistory(userId: string) {
    const withdrawals = await this.withdrawalModel
      .find({ userId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .lean();

    return withdrawals.map((w) => ({
      id: w._id,
      amount: w.amount,
      upiId: w.upiId,
      bankName: w.bankName,
      accountNumber: w.accountNumber,
      ifscCode: w.ifscCode,
      accountHolderName: w.accountHolderName,
      phoneNumber: w.phoneNumber,
      email: w.email,
      status: w.status,
      rejectionReason: w.rejectionReason,
      createdAt: (w as any).createdAt,
    }));
  }

  // ── Admin Methods ──

  /** Admin: get all withdrawals, paginated, filterable by status */
  async getAdminAll(page: number, limit: number, status?: string) {
    const skip = (page - 1) * limit;
    const filter: any = {};
    if (status && ['pending', 'approved', 'rejected'].includes(status)) {
      filter.status = status;
    }

    const [withdrawals, total, pendingCount, approvedTotal, rejectedTotal] = await Promise.all([
      this.withdrawalModel.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
      this.withdrawalModel.countDocuments(filter),
      this.withdrawalModel.countDocuments({ status: 'pending' }),
      this.withdrawalModel.aggregate([{ $match: { status: 'approved' } }, { $group: { _id: null, total: { $sum: '$amount' } } }]),
      this.withdrawalModel.countDocuments({ status: 'rejected' }),
    ]);

    // Populate user info
    const userIds = [...new Set(withdrawals.map((w) => w.userId.toString()))];
    const users = await this.userModel
      .find({ _id: { $in: userIds } })
      .select('name email')
      .lean();
    const userMap = new Map(users.map((u) => [u._id.toString(), u]));

    return {
      items: withdrawals.map((w) => {
        const user = userMap.get(w.userId.toString());
        return {
          _id: w._id,
          userName: user?.name || 'Unknown',
          userEmail: user?.email || '',
          amount: w.amount,
          upiId: w.upiId,
          bankName: w.bankName,
          accountNumber: w.accountNumber,
          ifscCode: w.ifscCode,
          accountHolderName: w.accountHolderName,
          phoneNumber: w.phoneNumber,
          email: w.email,
          status: w.status,
          rejectionReason: w.rejectionReason,
          createdAt: (w as any).createdAt,
        };
      }),
      total,
      page,
      pages: Math.ceil(total / limit),
      overview: {
        pending: pendingCount,
        totalApproved: approvedTotal[0]?.total || 0,
        totalRejected: rejectedTotal,
      },
    };
  }

  /** Admin: approve or reject a withdrawal */
  async updateStatus(id: string, status: WithdrawalStatus.APPROVED | WithdrawalStatus.REJECTED, reason?: string) {
    const withdrawal = await this.withdrawalModel.findById(id);
    if (!withdrawal) throw new BadRequestException('Withdrawal not found');
    if (withdrawal.status !== WithdrawalStatus.PENDING) {
      throw new BadRequestException(`Withdrawal already ${withdrawal.status}`);
    }

    withdrawal.status = status;
    if (status === WithdrawalStatus.REJECTED) {
      withdrawal.rejectionReason = reason || 'Rejected by admin';
      // Refund balance
      await this.walletService.addEarnings(withdrawal.userId.toString(), withdrawal.amount);
      this.logger.log(`Withdrawal ${id} rejected, ₹${withdrawal.amount} refunded`);
    } else {
      this.logger.log(`Withdrawal ${id} approved, ₹${withdrawal.amount} to ${withdrawal.accountHolderName || withdrawal.upiId}`);
    }
    await withdrawal.save();

    return { id: withdrawal._id, status: withdrawal.status };
  }
}
