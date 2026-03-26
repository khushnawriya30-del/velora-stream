import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AnalyticsController } from './analytics.controller';
import { AnalyticsService } from './analytics.service';
import { User, UserSchema } from '../../schemas/user.schema';
import { Movie, MovieSchema } from '../../schemas/movie.schema';
import { WatchProgress, WatchProgressSchema } from '../../schemas/watch-progress.schema';
import { Review, ReviewSchema } from '../../schemas/review.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: Movie.name, schema: MovieSchema },
      { name: WatchProgress.name, schema: WatchProgressSchema },
      { name: Review.name, schema: ReviewSchema },
    ]),
  ],
  controllers: [AnalyticsController],
  providers: [AnalyticsService],
})
export class AnalyticsModule {}
