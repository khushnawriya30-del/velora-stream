import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AdminController } from './admin.controller';
import { AdminLog, AdminLogSchema } from '../../schemas/admin-log.schema';
import { User, UserSchema } from '../../schemas/user.schema';
import { Movie, MovieSchema } from '../../schemas/movie.schema';
import { WatchProgress, WatchProgressSchema } from '../../schemas/watch-progress.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: AdminLog.name, schema: AdminLogSchema },
      { name: User.name, schema: UserSchema },
      { name: Movie.name, schema: MovieSchema },
      { name: WatchProgress.name, schema: WatchProgressSchema },
    ]),
  ],
  controllers: [AdminController],
})
export class AdminModule {}
