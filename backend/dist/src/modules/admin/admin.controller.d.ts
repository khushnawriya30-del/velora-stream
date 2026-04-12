import { Model, Types } from 'mongoose';
import { User, UserDocument } from '../../schemas/user.schema';
import { AdminLog, AdminLogDocument } from '../../schemas/admin-log.schema';
import { WatchProgress, WatchProgressDocument } from '../../schemas/watch-progress.schema';
import { WalletDocument } from '../../schemas/wallet.schema';
import { ReferralDocument } from '../../schemas/referral.schema';
import { ProfileDocument } from '../../schemas/profile.schema';
import { WatchlistDocument } from '../../schemas/watchlist.schema';
import { ReviewDocument } from '../../schemas/review.schema';
import { WithdrawalDocument } from '../../schemas/withdrawal.schema';
import { ContentViewDocument } from '../../schemas/content-view.schema';
export declare class AdminController {
    private userModel;
    private adminLogModel;
    private watchProgressModel;
    private walletModel;
    private referralModel;
    private profileModel;
    private watchlistModel;
    private reviewModel;
    private withdrawalModel;
    private contentViewModel;
    constructor(userModel: Model<UserDocument>, adminLogModel: Model<AdminLogDocument>, watchProgressModel: Model<WatchProgressDocument>, walletModel: Model<WalletDocument>, referralModel: Model<ReferralDocument>, profileModel: Model<ProfileDocument>, watchlistModel: Model<WatchlistDocument>, reviewModel: Model<ReviewDocument>, withdrawalModel: Model<WithdrawalDocument>, contentViewModel: Model<ContentViewDocument>);
    getUsers(page?: number, limit?: number, search?: string): Promise<{
        users: (import("mongoose").Document<unknown, {}, UserDocument, {}, {}> & User & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
            _id: Types.ObjectId;
        }> & {
            __v: number;
        })[];
        total: number;
        page: number;
        pages: number;
    }>;
    getUser(id: string): Promise<(import("mongoose").Document<unknown, {}, UserDocument, {}, {}> & User & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
        _id: Types.ObjectId;
    }> & {
        __v: number;
    }) | null>;
    suspendUser(id: string, reason: string, admin: any): Promise<(import("mongoose").Document<unknown, {}, UserDocument, {}, {}> & User & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
        _id: Types.ObjectId;
    }> & {
        __v: number;
    }) | null>;
    unsuspendUser(id: string, admin: any): Promise<(import("mongoose").Document<unknown, {}, UserDocument, {}, {}> & User & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
        _id: Types.ObjectId;
    }> & {
        __v: number;
    }) | null>;
    deleteUser(id: string, admin: any): Promise<{
        message: string;
    }>;
    getLogs(page?: number, limit?: number): Promise<{
        logs: (import("mongoose").Document<unknown, {}, AdminLogDocument, {}, {}> & AdminLog & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
            _id: Types.ObjectId;
        }> & {
            __v: number;
        })[];
        total: number;
    }>;
    getUserWatchHistory(userId: string, page?: number, limit?: number): Promise<{
        items: (import("mongoose").Document<unknown, {}, WatchProgressDocument, {}, {}> & WatchProgress & import("mongoose").Document<Types.ObjectId, any, any, Record<string, any>, {}> & Required<{
            _id: Types.ObjectId;
        }> & {
            __v: number;
        })[];
        total: number;
        page: number;
    }>;
}
