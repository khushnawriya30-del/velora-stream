import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { SearchController } from './search.controller';
import { SearchService } from './search.service';
import { Movie, MovieSchema } from '../../schemas/movie.schema';
import { SearchQuery, SearchQuerySchema } from '../../schemas/search-query.schema';
import { Season, SeasonSchema, Episode, EpisodeSchema } from '../../schemas/series.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Movie.name, schema: MovieSchema },
      { name: SearchQuery.name, schema: SearchQuerySchema },
      { name: Season.name, schema: SeasonSchema },
      { name: Episode.name, schema: EpisodeSchema },
    ]),
  ],
  controllers: [SearchController],
  providers: [SearchService],
})
export class SearchModule {}
