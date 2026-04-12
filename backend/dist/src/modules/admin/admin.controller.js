"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AdminController = void 0;
const common_1 = require("@nestjs/common");
const passport_1 = require("@nestjs/passport");
const mongoose_1 = require("@nestjs/mongoose");
const mongoose_2 = require("mongoose");
const swagger_1 = require("@nestjs/swagger");
const roles_decorator_1 = require("../auth/decorators/roles.decorator");
const roles_guard_1 = require("../auth/guards/roles.guard");
const current_user_decorator_1 = require("../auth/decorators/current-user.decorator");
const user_schema_1 = require("../../schemas/user.schema");
const admin_log_schema_1 = require("../../schemas/admin-log.schema");
const watch_progress_schema_1 = require("../../schemas/watch-progress.schema");
const wallet_schema_1 = require("../../schemas/wallet.schema");
const referral_schema_1 = require("../../schemas/referral.schema");
const profile_schema_1 = require("../../schemas/profile.schema");
const watchlist_schema_1 = require("../../schemas/watchlist.schema");
const review_schema_1 = require("../../schemas/review.schema");
const withdrawal_schema_1 = require("../../schemas/withdrawal.schema");
const content_view_schema_1 = require("../../schemas/content-view.schema");
let AdminController = class AdminController {
    constructor(userModel, adminLogModel, watchProgressModel, walletModel, referralModel, profileModel, watchlistModel, reviewModel, withdrawalModel, contentViewModel) {
        this.userModel = userModel;
        this.adminLogModel = adminLogModel;
        this.watchProgressModel = watchProgressModel;
        this.walletModel = walletModel;
        this.referralModel = referralModel;
        this.profileModel = profileModel;
        this.watchlistModel = watchlistModel;
        this.reviewModel = reviewModel;
        this.withdrawalModel = withdrawalModel;
        this.contentViewModel = contentViewModel;
    }
    async getUsers(page = 1, limit = 20, search) {
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
    async getUser(id) {
        return this.userModel.findById(id).select('-password -passwordResetToken');
    }
    async suspendUser(id, reason, admin) {
        const user = await this.userModel.findByIdAndUpdate(id, { isSuspended: true, suspendReason: reason }, { new: true });
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
    async unsuspendUser(id, admin) {
        const user = await this.userModel.findByIdAndUpdate(id, { isSuspended: false, suspendReason: null }, { new: true });
        await this.adminLogModel.create({
            adminId: admin.userId,
            adminEmail: admin.email,
            action: 'unsuspend_user',
            resource: 'user',
            resourceId: id,
        });
        return user;
    }
    async deleteUser(id, admin) {
        const user = await this.userModel.findById(id);
        if (!user)
            return { message: 'User not found' };
        const userName = user.name || user.email;
        const oid = new mongoose_2.Types.ObjectId(id);
        await Promise.all([
            this.watchProgressModel.deleteMany({ userId: oid }),
            this.walletModel.deleteMany({ userId: oid }),
            this.referralModel.deleteMany({ $or: [{ referrerId: oid }, { newUserId: oid }] }),
            this.profileModel.deleteMany({ userId: oid }),
            this.watchlistModel.deleteMany({ userId: oid }),
            this.reviewModel.deleteMany({ userId: oid }),
            this.withdrawalModel.deleteMany({ userId: oid }),
            this.contentViewModel.deleteMany({ userId: oid }),
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
        return { message: `User ${userName} and all related data deleted successfully` };
    }
    async getLogs(page = 1, limit = 50) {
        const skip = (Number(page) - 1) * Number(limit);
        const [logs, total] = await Promise.all([
            this.adminLogModel.find().sort({ createdAt: -1 }).skip(skip).limit(Number(limit)),
            this.adminLogModel.countDocuments(),
        ]);
        return { logs, total };
    }
    async getUserWatchHistory(userId, page = 1, limit = 20) {
        const skip = (Number(page) - 1) * Number(limit);
        const filter = { userId: new mongoose_2.Types.ObjectId(userId) };
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
};
exports.AdminController = AdminController;
__decorate([
    (0, common_1.Get)('users'),
    (0, swagger_1.ApiOperation)({ summary: 'List all users (Admin)' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __param(2, (0, common_1.Query)('search')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object, String]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getUsers", null);
__decorate([
    (0, common_1.Get)('users/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get user details (Admin)' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getUser", null);
__decorate([
    (0, common_1.Patch)('users/:id/suspend'),
    (0, swagger_1.ApiOperation)({ summary: 'Suspend a user (Admin)' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)('reason')),
    __param(2, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "suspendUser", null);
__decorate([
    (0, common_1.Patch)('users/:id/unsuspend'),
    (0, swagger_1.ApiOperation)({ summary: 'Unsuspend a user (Admin)' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "unsuspendUser", null);
__decorate([
    (0, common_1.Delete)('users/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete a user permanently (Admin)' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, current_user_decorator_1.CurrentUser)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "deleteUser", null);
__decorate([
    (0, common_1.Get)('logs'),
    (0, swagger_1.ApiOperation)({ summary: 'Get admin audit logs' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getLogs", null);
__decorate([
    (0, common_1.Get)('users/:id/watch-history'),
    (0, swagger_1.ApiOperation)({ summary: 'Get watch history for a specific user (Admin)' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Query)('page')),
    __param(2, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, Object]),
    __metadata("design:returntype", Promise)
], AdminController.prototype, "getUserWatchHistory", null);
exports.AdminController = AdminController = __decorate([
    (0, swagger_1.ApiTags)('Admin'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    (0, common_1.Controller)('admin'),
    __param(0, (0, mongoose_1.InjectModel)(user_schema_1.User.name)),
    __param(1, (0, mongoose_1.InjectModel)(admin_log_schema_1.AdminLog.name)),
    __param(2, (0, mongoose_1.InjectModel)(watch_progress_schema_1.WatchProgress.name)),
    __param(3, (0, mongoose_1.InjectModel)(wallet_schema_1.Wallet.name)),
    __param(4, (0, mongoose_1.InjectModel)(referral_schema_1.Referral.name)),
    __param(5, (0, mongoose_1.InjectModel)(profile_schema_1.Profile.name)),
    __param(6, (0, mongoose_1.InjectModel)(watchlist_schema_1.Watchlist.name)),
    __param(7, (0, mongoose_1.InjectModel)(review_schema_1.Review.name)),
    __param(8, (0, mongoose_1.InjectModel)(withdrawal_schema_1.Withdrawal.name)),
    __param(9, (0, mongoose_1.InjectModel)(content_view_schema_1.ContentView.name)),
    __metadata("design:paramtypes", [mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model,
        mongoose_2.Model])
], AdminController);
//# sourceMappingURL=admin.controller.js.map