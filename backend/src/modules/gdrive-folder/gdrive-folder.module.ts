import { Module } from '@nestjs/common';
import { GdriveFolderController } from './gdrive-folder.controller';
import { GdriveFolderService } from './gdrive-folder.service';
import { SeriesModule } from '../series/series.module';

@Module({
  imports: [SeriesModule],
  controllers: [GdriveFolderController],
  providers: [GdriveFolderService],
})
export class GdriveFolderModule {}
