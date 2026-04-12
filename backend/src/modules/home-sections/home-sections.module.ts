import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { HomeSectionsController } from './home-sections.controller';
import { HomeSectionsService } from './home-sections.service';
import { HomeSection, HomeSectionSchema } from '../../schemas/home-section.schema';
import { Movie, MovieSchema } from '../../schemas/movie.schema';
import { Banner, BannerSchema } from '../../schemas/banner.schema';
import { Season, SeasonSchema, Episode, EpisodeSchema } from '../../schemas/series.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: HomeSection.name, schema: HomeSectionSchema },
      { name: Movie.name, schema: MovieSchema },
      { name: Banner.name, schema: BannerSchema },
      { name: Season.name, schema: SeasonSchema },
      { name: Episode.name, schema: EpisodeSchema },
    ]),
  ],
  controllers: [HomeSectionsController],
  providers: [HomeSectionsService],
  exports: [HomeSectionsService],
})
export class HomeSectionsModule {}
