import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AdminController } from './admin.controller';
import { AdminLog, AdminLogSchema } from '../../schemas/admin-log.schema';
import { User, UserSchema } from '../../schemas/user.schema';
import { Movie, MovieSchema } from '../../schemas/movie.schema';
import { WatchProgress, WatchProgressSchema } from '../../schemas/watch-progress.schema';
import { Wallet, WalletSchema } from '../../schemas/wallet.schema';
import { Referral, ReferralSchema } from '../../schemas/referral.schema';
import { Profile, ProfileSchema } from '../../schemas/profile.schema';
import { Watchlist, WatchlistSchema } from '../../schemas/watchlist.schema';
import { Review, ReviewSchema } from '../../schemas/review.schema';
import { Withdrawal, WithdrawalSchema } from '../../schemas/withdrawal.schema';
import { ContentView, ContentViewSchema } from '../../schemas/content-view.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: AdminLog.name, schema: AdminLogSchema },
      { name: User.name, schema: UserSchema },
      { name: Movie.name, schema: MovieSchema },
      { name: WatchProgress.name, schema: WatchProgressSchema },
      { name: Wallet.name, schema: WalletSchema },
      { name: Referral.name, schema: ReferralSchema },
      { name: Profile.name, schema: ProfileSchema },
      { name: Watchlist.name, schema: WatchlistSchema },
      { name: Review.name, schema: ReviewSchema },
      { name: Withdrawal.name, schema: WithdrawalSchema },
      { name: ContentView.name, schema: ContentViewSchema },
    ]),
  ],
  controllers: [AdminController],
})
export class AdminModule {}
