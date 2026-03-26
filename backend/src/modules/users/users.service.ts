import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { User, UserDocument } from '../../schemas/user.schema';

@Injectable()
export class UsersService {
  constructor(@InjectModel(User.name) private userModel: Model<UserDocument>) {}

  async findById(id: string): Promise<UserDocument> {
    const user = await this.userModel.findById(id);
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async findAll(page = 1, limit = 20): Promise<{ users: UserDocument[]; total: number }> {
    const skip = (page - 1) * limit;
    const [users, total] = await Promise.all([
      this.userModel.find().sort({ createdAt: -1 }).skip(skip).limit(limit),
      this.userModel.countDocuments(),
    ]);
    return { users, total };
  }

  async updateProfile(userId: string, updates: Partial<User>): Promise<UserDocument> {
    const user = await this.userModel.findByIdAndUpdate(userId, updates, { new: true });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async suspendUser(userId: string, reason: string): Promise<UserDocument> {
    const user = await this.userModel.findByIdAndUpdate(
      userId,
      { isSuspended: true, suspendReason: reason },
      { new: true },
    );
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async unsuspendUser(userId: string): Promise<UserDocument> {
    const user = await this.userModel.findByIdAndUpdate(
      userId,
      { isSuspended: false, suspendReason: null },
      { new: true },
    );
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async updateFcmToken(userId: string, token: string): Promise<void> {
    await this.userModel.findByIdAndUpdate(userId, {
      $addToSet: { fcmTokens: token },
    });
  }

  async removeFcmToken(userId: string, token: string): Promise<void> {
    await this.userModel.findByIdAndUpdate(userId, {
      $pull: { fcmTokens: token },
    });
  }

  async getStats(): Promise<{ total: number; active: number; suspended: number }> {
    const [total, suspended] = await Promise.all([
      this.userModel.countDocuments(),
      this.userModel.countDocuments({ isSuspended: true }),
    ]);
    return { total, active: total - suspended, suspended };
  }
}
