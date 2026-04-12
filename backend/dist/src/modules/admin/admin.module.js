"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AdminModule = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const admin_controller_1 = require("./admin.controller");
const admin_log_schema_1 = require("../../schemas/admin-log.schema");
const user_schema_1 = require("../../schemas/user.schema");
const movie_schema_1 = require("../../schemas/movie.schema");
const watch_progress_schema_1 = require("../../schemas/watch-progress.schema");
const wallet_schema_1 = require("../../schemas/wallet.schema");
const referral_schema_1 = require("../../schemas/referral.schema");
const profile_schema_1 = require("../../schemas/profile.schema");
const watchlist_schema_1 = require("../../schemas/watchlist.schema");
const review_schema_1 = require("../../schemas/review.schema");
const withdrawal_schema_1 = require("../../schemas/withdrawal.schema");
const content_view_schema_1 = require("../../schemas/content-view.schema");
let AdminModule = class AdminModule {
};
exports.AdminModule = AdminModule;
exports.AdminModule = AdminModule = __decorate([
    (0, common_1.Module)({
        imports: [
            mongoose_1.MongooseModule.forFeature([
                { name: admin_log_schema_1.AdminLog.name, schema: admin_log_schema_1.AdminLogSchema },
                { name: user_schema_1.User.name, schema: user_schema_1.UserSchema },
                { name: movie_schema_1.Movie.name, schema: movie_schema_1.MovieSchema },
                { name: watch_progress_schema_1.WatchProgress.name, schema: watch_progress_schema_1.WatchProgressSchema },
                { name: wallet_schema_1.Wallet.name, schema: wallet_schema_1.WalletSchema },
                { name: referral_schema_1.Referral.name, schema: referral_schema_1.ReferralSchema },
                { name: profile_schema_1.Profile.name, schema: profile_schema_1.ProfileSchema },
                { name: watchlist_schema_1.Watchlist.name, schema: watchlist_schema_1.WatchlistSchema },
                { name: review_schema_1.Review.name, schema: review_schema_1.ReviewSchema },
                { name: withdrawal_schema_1.Withdrawal.name, schema: withdrawal_schema_1.WithdrawalSchema },
                { name: content_view_schema_1.ContentView.name, schema: content_view_schema_1.ContentViewSchema },
            ]),
        ],
        controllers: [admin_controller_1.AdminController],
    })
], AdminModule);
//# sourceMappingURL=admin.module.js.map