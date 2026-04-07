import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Wallet, WalletDocument } from '../../schemas/wallet.schema';

@Injectable()
export class WalletService {
  private readonly logger = new Logger(WalletService.name);

  constructor(
    @InjectModel(Wallet.name) private walletModel: Model<WalletDocument>,
  ) {}

  /** Get or create wallet for a user (auto-creates with ₹80 default) */
  async getOrCreateWallet(userId: string): Promise<WalletDocument> {
    let wallet = await this.walletModel.findOne({ userId: new Types.ObjectId(userId) });
    if (!wallet) {
      wallet = await this.walletModel.create({
        userId: new Types.ObjectId(userId),
        balance: 80,
        totalEarned: 80,
      });
      this.logger.log(`Created wallet for user ${userId} with ₹80 default balance`);
    }
    return wallet;
  }

  /** Add earnings to wallet (e.g., from referral) */
  async addEarnings(userId: string, amount: number): Promise<WalletDocument> {
    const wallet = await this.getOrCreateWallet(userId);
    wallet.balance += amount;
    wallet.totalEarned += amount;
    wallet.totalReferrals += 1;
    await wallet.save();
    this.logger.log(`Added ₹${amount} to wallet of user ${userId}. New balance: ₹${wallet.balance}`);
    return wallet;
  }

  /** Deduct from wallet for withdrawal */
  async deductBalance(userId: string, amount: number): Promise<WalletDocument> {
    const wallet = await this.getOrCreateWallet(userId);
    if (wallet.balance < amount) {
      throw new Error('Insufficient balance');
    }
    wallet.balance -= amount;
    wallet.totalWithdrawn += amount;
    await wallet.save();
    return wallet;
  }

  /** Get balance summary */
  async getBalance(userId: string) {
    const wallet = await this.getOrCreateWallet(userId);
    const remaining = Math.max(0, wallet.withdrawThreshold - wallet.balance);
    return {
      balance: wallet.balance,
      totalEarned: wallet.totalEarned,
      totalWithdrawn: wallet.totalWithdrawn,
      totalReferrals: wallet.totalReferrals,
      withdrawThreshold: wallet.withdrawThreshold,
      canWithdraw: wallet.balance >= wallet.withdrawThreshold,
      amountNeeded: remaining,
    };
  }
}
