import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TranscodeController } from './transcode.controller';
import { TranscodeService } from './transcode.service';
import { Movie, MovieSchema } from '../../schemas/movie.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Movie.name, schema: MovieSchema }]),
  ],
  controllers: [TranscodeController],
  providers: [TranscodeService],
})
export class TranscodeModule {}
