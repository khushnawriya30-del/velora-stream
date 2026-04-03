import { Controller, Get, Post, Patch, Delete, Param, Body, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { User, UserDocument } from '../../schemas/user.schema';
import { AdminLog, AdminLogDocument } from '../../schemas/admin-log.schema';
import { WatchProgress, WatchProgressDocument } from '../../schemas/watch-progress.schema';

@ApiTags('Admin')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin')
@Controller('admin')
export class AdminController {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    @InjectModel(AdminLog.name) private adminLogModel: Model<AdminLogDocument>,
    @InjectModel(WatchProgress.name) private watchProgressModel: Model<WatchProgressDocument>,
  ) {}

  @Get('users')
  @ApiOperation({ summary: 'List all users (Admin)' })
  async getUsers(@Query('page') page = 1, @Query('limit') limit = 20, @Query('search') search?: string) {
    const skip = (Number(page) - 1) * Number(limit);
    const filter = search
      ? { $or: [{ name: { $regex: search, $options: 'i' } }, { email: { $regex: search, $options: 'i' } }] }
      : {};
    const [users, total] = await Promise.all([
      this.userModel.find(filter).sort({ createdAt: -1 }).skip(skip).limit(Number(limit))
        .select('name email role authProvider createdAt lastActiveAt isSuspended deviceInfo'),
      this.userModel.countDocuments(filter),
    ]);
    return { users, total, page: Number(page), pages: Math.ceil(total / Number(limit)) };
  }

  @Get('users/:id')
  @ApiOperation({ summary: 'Get user details (Admin)' })
  async getUser(@Param('id') id: string) {
    return this.userModel.findById(id).select('-password -passwordResetToken');
  }

  @Patch('users/:id/suspend')
  @ApiOperation({ summary: 'Suspend a user (Admin)' })
  async suspendUser(
    @Param('id') id: string,
    @Body('reason') reason: string,
    @CurrentUser() admin: any,
  ) {
    const user = await this.userModel.findByIdAndUpdate(
      id,
      { isSuspended: true, suspendReason: reason },
      { new: true },
    );
    await this.adminLogModel.create({
      adminId: admin.userId,
      adminEmail: admin.email,
      action: 'suspend_user',
      resource: 'user',
      resourceId: id,
      details: { reason },
    });
    return user;
  }

  @Patch('users/:id/unsuspend')
  @ApiOperation({ summary: 'Unsuspend a user (Admin)' })
  async unsuspendUser(@Param('id') id: string, @CurrentUser() admin: any) {
    const user = await this.userModel.findByIdAndUpdate(
      id,
      { isSuspended: false, suspendReason: null },
      { new: true },
    );
    await this.adminLogModel.create({
      adminId: admin.userId,
      adminEmail: admin.email,
      action: 'unsuspend_user',
      resource: 'user',
      resourceId: id,
    });
    return user;
  }

  @Delete('users/:id')
  @ApiOperation({ summary: 'Delete a user permanently (Admin)' })
  async deleteUser(@Param('id') id: string, @CurrentUser() admin: any) {
    const user = await this.userModel.findById(id);
    if (!user) return { message: 'User not found' };
    const userName = user.name || user.email;
    await Promise.all([
      this.watchProgressModel.deleteMany({ userId: new Types.ObjectId(id) }),
      this.userModel.findByIdAndDelete(id),
    ]);
    await this.adminLogModel.create({
      adminId: admin.userId,
      adminEmail: admin.email,
      action: 'delete_user',
      resource: 'user',
      resourceId: id,
      details: { userName },
    });
    return { message: `User ${userName} deleted successfully` };
  }

  @Get('logs')
  @ApiOperation({ summary: 'Get admin audit logs' })
  async getLogs(@Query('page') page = 1, @Query('limit') limit = 50) {
    const skip = (Number(page) - 1) * Number(limit);
    const [logs, total] = await Promise.all([
      this.adminLogModel.find().sort({ createdAt: -1 }).skip(skip).limit(Number(limit)),
      this.adminLogModel.countDocuments(),
    ]);
    return { logs, total };
  }

  @Get('users/:id/watch-history')
  @ApiOperation({ summary: 'Get watch history for a specific user (Admin)' })
  async getUserWatchHistory(
    @Param('id') userId: string,
    @Query('page') page = 1,
    @Query('limit') limit = 20,
  ) {
    const skip = (Number(page) - 1) * Number(limit);
    const filter = { userId: new Types.ObjectId(userId) };
    const [items, total] = await Promise.all([
      this.watchProgressModel
        .find(filter)
        .sort({ lastWatchedAt: -1 })
        .skip(skip)
        .limit(Number(limit)),
      this.watchProgressModel.countDocuments(filter),
    ]);
    return { items, total, page: Number(page) };
  }
}
