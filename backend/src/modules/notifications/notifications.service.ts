import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Notification, NotificationDocument } from '../../schemas/notification.schema';
import { User, UserDocument } from '../../schemas/user.schema';

@Injectable()
export class NotificationsService {
  constructor(
    @InjectModel(Notification.name) private notificationModel: Model<NotificationDocument>,
    @InjectModel(User.name) private userModel: Model<UserDocument>,
  ) {}

  async create(data: Partial<Notification>): Promise<NotificationDocument> {
    return this.notificationModel.create(data);
  }

  async getAll(page = 1, limit = 20): Promise<{ notifications: NotificationDocument[]; total: number }> {
    const skip = (page - 1) * limit;
    const [notifications, total] = await Promise.all([
      this.notificationModel.find().sort({ createdAt: -1 }).skip(skip).limit(limit),
      this.notificationModel.countDocuments(),
    ]);
    return { notifications, total };
  }

  async send(notificationId: string): Promise<NotificationDocument> {
    const notification = await this.notificationModel.findById(notificationId);
    if (!notification) throw new NotFoundException('Notification not found');

    // In production: use Firebase Admin SDK to send push notifications
    // const tokens = await this.collectTargetTokens(notification);
    // await admin.messaging().sendEachForMulticast({ tokens, notification: { title, body } });

    notification.isSent = true;
    notification.sentAt = new Date();
    await notification.save();

    return notification;
  }

  async delete(id: string): Promise<void> {
    const result = await this.notificationModel.findByIdAndDelete(id);
    if (!result) throw new NotFoundException('Notification not found');
  }
}
