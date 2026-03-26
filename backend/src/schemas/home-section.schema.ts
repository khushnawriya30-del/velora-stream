import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type HomeSectionDocument = HomeSection & Document;

export enum SectionType {
  STANDARD = 'standard', // Normal horizontal scrolling
  LARGE_CARD = 'large_card', // Bigger cards
  MID_BANNER = 'mid_banner', // Square banner between sections
  TRENDING = 'trending', // Top 10 with numbers
}

export enum CardSize {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large',
}

@Schema({ timestamps: true })
export class HomeSection {
  @Prop({ required: true, trim: true })
  title: string;

  @Prop()
  slug: string;

  @Prop({ required: true, enum: SectionType, default: SectionType.STANDARD })
  type: SectionType;

  @Prop({ type: [Types.ObjectId], ref: 'Movie' })
  contentIds: Types.ObjectId[];

  @Prop({ default: 0 })
  displayOrder: number;

  @Prop({ default: true })
  isVisible: boolean;

  @Prop({ default: false })
  isSystemManaged: boolean; // e.g., "Trending", "New Releases" auto-populated

  @Prop()
  contentType: string; // filter: movie, web_series, etc.

  @Prop()
  genre: string; // filter by genre

  @Prop()
  sortBy: string; // popularityScore, createdAt, rating, viewCount

  @Prop({ default: 20 })
  maxItems: number;

  @Prop({ enum: CardSize, default: CardSize.SMALL })
  cardSize: CardSize;

  @Prop({ default: true })
  showViewMore: boolean;

  @Prop({ default: 'View More' })
  viewMoreText: string;

  @Prop()
  bannerImageUrl: string; // For mid_banner type

  @Prop({ default: false })
  showTrendingNumbers: boolean; // For trending type - shows 1, 2, 3...

  @Prop([String])
  tags: string[]; // For categorizing sections
}

export const HomeSectionSchema = SchemaFactory.createForClass(HomeSection);
HomeSectionSchema.index({ displayOrder: 1, isVisible: 1 });
HomeSectionSchema.index({ type: 1 });
