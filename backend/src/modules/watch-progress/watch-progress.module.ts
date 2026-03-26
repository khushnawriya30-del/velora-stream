import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { WatchProgressController } from './watch-progress.controller';
import { WatchProgressService } from './watch-progress.service';
import { WatchProgress, WatchProgressSchema } from '../../schemas/watch-progress.schema';

@Module({
  imports: [MongooseModule.forFeature([{ name: WatchProgress.name, schema: WatchProgressSchema }])],
  controllers: [WatchProgressController],
  providers: [WatchProgressService],
  exports: [WatchProgressService],
})
export class WatchProgressModule {}
