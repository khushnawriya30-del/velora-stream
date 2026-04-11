import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type SearchQueryDocument = SearchQuery & Document;

@Schema({ timestamps: true })
export class SearchQuery {
  @Prop({ required: true, index: true })
  query: string;

  @Prop({ default: 1 })
  count: number;

  @Prop({ type: String, default: null })
  contentId: string;

  @Prop({ type: String, default: null })
  posterUrl: string;

  @Prop({ type: String, default: null })
  contentType: string;
}

export const SearchQuerySchema = SchemaFactory.createForClass(SearchQuery);
SearchQuerySchema.index({ count: -1 });
SearchQuerySchema.index({ query: 1 }, { unique: true });
