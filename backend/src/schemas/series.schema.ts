import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';
import { StreamingSource, StreamingSourceSchema } from './movie.schema';

export type SeasonDocument = Season & Document;
export type EpisodeDocument = Episode & Document;

@Schema({ _id: false })
export class SkipTimestamp {
  @Prop({ required: true })
  start: number; // seconds

  @Prop({ required: true })
  end: number; // seconds
}

export const SkipTimestampSchema = SchemaFactory.createForClass(SkipTimestamp);

@Schema({ _id: false })
export class SubtitleTrack {
  @Prop({ required: true })
  language: string;

  @Prop({ required: true })
  url: string;

  @Prop({ default: false })
  isDefault: boolean;
}

export const SubtitleTrackSchema = SchemaFactory.createForClass(SubtitleTrack);

@Schema({ _id: false })
export class AudioTrack {
  @Prop({ required: true })
  language: string;

  @Prop()
  label: string; // e.g., "Hindi [Original]"

  @Prop({ default: false })
  isDefault: boolean;
}

export const AudioTrackSchema = SchemaFactory.createForClass(AudioTrack);

@Schema({ timestamps: true })
export class Episode {
  @Prop({ type: Types.ObjectId, ref: 'Season', required: true })
  seasonId: Types.ObjectId;

  @Prop({ required: true })
  episodeNumber: number;

  @Prop({ required: true, trim: true })
  title: string;

  @Prop()
  synopsis: string;

  @Prop()
  duration: number; // minutes

  @Prop()
  airDate: Date;

  @Prop()
  thumbnailUrl: string;

  @Prop({ type: [StreamingSourceSchema] })
  streamingSources: StreamingSource[];

  @Prop({ type: SkipTimestampSchema })
  skipIntro: SkipTimestamp;

  @Prop({ type: SkipTimestampSchema })
  skipRecap: SkipTimestamp;

  @Prop({ type: [SubtitleTrackSchema] })
  subtitles: SubtitleTrack[];

  @Prop({ type: [AudioTrackSchema] })
  audioTracks: AudioTrack[];

  @Prop({ default: 0 })
  viewCount: number;

  @Prop({ default: false })
  isPremium: boolean; // Premium-only episode
}

export const EpisodeSchema = SchemaFactory.createForClass(Episode);
EpisodeSchema.index({ seasonId: 1, episodeNumber: 1 });

@Schema({ timestamps: true })
export class Season {
  @Prop({ type: Types.ObjectId, ref: 'Movie', required: true })
  seriesId: Types.ObjectId;

  @Prop({ required: true })
  seasonNumber: number;

  @Prop()
  title: string;

  @Prop()
  synopsis: string;

  @Prop()
  posterUrl: string;

  @Prop()
  releaseYear: number;

  @Prop({ default: 0 })
  episodeCount: number;

  @Prop({ default: false })
  isPremium: boolean;
}

export const SeasonSchema = SchemaFactory.createForClass(Season);
SeasonSchema.index({ seriesId: 1, seasonNumber: 1 });
