import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Wallet, WalletDocument } from '../../schemas/wallet.schema';
import { InviteSettings, InviteSettingsDocument } from '../../schemas/invite-settings.schema';

@Injectable()
export class WalletService {
  private readonly logger = new Logger(WalletService.name);

  constructor(
    @InjectModel(Wallet.name) private walletModel: Model<WalletDocument>,
    @InjectModel(InviteSettings.name) private inviteSettingsModel: Model<InviteSettingsDocument>,
  ) {}

  /** Get current default balance from invite settings */
  private async getDefaultBalance(): Promise<number> {
    const settings = await this.inviteSettingsModel.findOne({ key: 'default' });
    return settings?.defaultBalance ?? 80;
  }

  /** Get or create wallet for a user (auto-creates with dynamic default balance) */
  async getOrCreateWallet(userId: string): Promise<WalletDocument> {
    let wallet = await this.walletModel.findOne({ userId: new Types.ObjectId(userId) });
    if (!wallet) {
      const defaultBalance = await this.getDefaultBalance();
      wallet = await this.walletModel.create({
        userId: new Types.ObjectId(userId),
        balance: defaultBalance,
        totalEarned: defaultBalance,
      });
      this.logger.log(`Created wallet for user ${userId} with ₹${defaultBalance} default balance`);
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

  /** Save bank details for a user (one-time save) */
  async saveBankDetails(userId: string, details: {
    bankName: string;
    accountNumber: string;
    ifscCode: string;
    accountHolderName: string;
    phoneNumber: string;
    bankEmail: string;
  }) {
    const wallet = await this.getOrCreateWallet(userId);
    wallet.bankName = details.bankName;
    wallet.accountNumber = details.accountNumber;
    wallet.ifscCode = details.ifscCode;
    wallet.accountHolderName = details.accountHolderName;
    wallet.phoneNumber = details.phoneNumber;
    wallet.bankEmail = details.bankEmail;
    await wallet.save();
    this.logger.log(`Saved bank details for user ${userId}`);
    return {
      bankName: wallet.bankName,
      accountNumber: wallet.accountNumber,
      ifscCode: wallet.ifscCode,
      accountHolderName: wallet.accountHolderName,
      phoneNumber: wallet.phoneNumber,
      email: wallet.bankEmail,
    };
  }

  /** Get saved bank details for a user */
  async getBankDetails(userId: string) {
    const wallet = await this.getOrCreateWallet(userId);
    return {
      bankName: wallet.bankName || '',
      accountNumber: wallet.accountNumber || '',
      ifscCode: wallet.ifscCode || '',
      accountHolderName: wallet.accountHolderName || '',
      phoneNumber: wallet.phoneNumber || '',
      email: wallet.bankEmail || '',
      hasBankDetails: !!(wallet.bankName && wallet.accountNumber && wallet.ifscCode),
    };
  }

  /** Admin: get bank details for a specific user */
  async getAdminUserBankDetails(userId: string) {
    const wallet = await this.walletModel.findOne({ userId: new Types.ObjectId(userId) }).lean();
    if (!wallet) return null;
    return {
      bankName: wallet.bankName || '',
      accountNumber: wallet.accountNumber || '',
      ifscCode: wallet.ifscCode || '',
      accountHolderName: wallet.accountHolderName || '',
      phoneNumber: wallet.phoneNumber || '',
      email: wallet.bankEmail || '',
      balance: wallet.balance,
      totalEarned: wallet.totalEarned,
      totalWithdrawn: wallet.totalWithdrawn,
      totalReferrals: wallet.totalReferrals,
    };
  }
}
