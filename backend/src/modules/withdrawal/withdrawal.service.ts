import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Withdrawal, WithdrawalDocument } from '../../schemas/withdrawal.schema';
import { WalletService } from '../wallet/wallet.service';

@Injectable()
export class WithdrawalService {
  private readonly logger = new Logger(WithdrawalService.name);

  constructor(
    @InjectModel(Withdrawal.name) private withdrawalModel: Model<WithdrawalDocument>,
    private readonly walletService: WalletService,
  ) {}

  /** Request a withdrawal */
  async requestWithdrawal(userId: string, amount: number, upiId: string) {
    if (!upiId || !upiId.includes('@')) {
      throw new BadRequestException('Invalid UPI ID');
    }
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

    // Deduct balance
    await this.walletService.deductBalance(userId, amount);

    // Create withdrawal request
    const withdrawal = await this.withdrawalModel.create({
      userId: new Types.ObjectId(userId),
      amount,
      upiId,
      status: 'pending',
    });

    this.logger.log(`Withdrawal request ₹${amount} by user ${userId} to ${upiId}`);
    return {
      id: withdrawal._id,
      amount: withdrawal.amount,
      upiId: withdrawal.upiId,
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
      status: w.status,
      createdAt: (w as any).createdAt,
    }));
  }
}
