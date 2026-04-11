import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ThematicCollectionController } from './thematic-collection.controller';
import { ThematicCollectionService } from './thematic-collection.service';
import { ThematicCollection, ThematicCollectionSchema } from '../../schemas/thematic-collection.schema';

@Module({
  imports: [MongooseModule.forFeature([{ name: ThematicCollection.name, schema: ThematicCollectionSchema }])],
  controllers: [ThematicCollectionController],
  providers: [ThematicCollectionService],
  exports: [ThematicCollectionService],
})
export class ThematicCollectionModule {}
