import {
  IsString, IsNotEmpty, IsOptional, IsArray, IsEnum, IsNumber, IsBoolean, IsUrl, IsDate, ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { ContentType, ContentRating, ContentStatus } from '../../../schemas/movie.schema';

class CastMemberDto {
  @ApiProperty() @IsString() @IsNotEmpty() name: string;
  @ApiPropertyOptional() @IsString() @IsOptional() role?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() character?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() photoUrl?: string;
}

class StreamingSourceDto {
  @ApiProperty() @IsString() @IsNotEmpty() label: string;
  @ApiProperty() @IsString() @IsNotEmpty() url: string;
  @ApiPropertyOptional() @IsString() @IsOptional() quality?: string;
  @ApiPropertyOptional() @IsNumber() @IsOptional() priority?: number;
}

export class CreateMovieDto {
  @ApiProperty() @IsString() @IsNotEmpty() title: string;
  @ApiPropertyOptional() @IsString() @IsOptional() alternateTitle?: string;
  @ApiProperty() @IsString() @IsNotEmpty() synopsis: string;

  @ApiProperty({ enum: ContentType })
  @IsEnum(ContentType)
  contentType: ContentType;

  @ApiProperty({ type: [String] }) @IsArray() @IsString({ each: true }) genres: string[];
  @ApiPropertyOptional({ type: [String] }) @IsArray() @IsString({ each: true }) @IsOptional() languages?: string[];

  @ApiPropertyOptional({ enum: ContentRating })
  @IsEnum(ContentRating) @IsOptional()
  contentRating?: ContentRating;

  @ApiPropertyOptional({ enum: ContentStatus })
  @IsEnum(ContentStatus) @IsOptional()
  status?: ContentStatus;

  @ApiPropertyOptional() @IsNumber() @IsOptional() releaseYear?: number;
  @ApiPropertyOptional() @IsString() @IsOptional() country?: string;
  @ApiPropertyOptional() @IsNumber() @IsOptional() duration?: number;
  @ApiPropertyOptional() @IsString() @IsOptional() director?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() studio?: string;

  @ApiPropertyOptional({ type: [CastMemberDto] })
  @ValidateNested({ each: true }) @Type(() => CastMemberDto) @IsOptional()
  cast?: CastMemberDto[];

  @ApiPropertyOptional() @IsString() @IsOptional() posterUrl?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() bannerUrl?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() logoUrl?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() trailerUrl?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() cbfcCertificateUrl?: string;

  @ApiPropertyOptional({ type: [StreamingSourceDto] })
  @ValidateNested({ each: true }) @Type(() => StreamingSourceDto) @IsOptional()
  streamingSources?: StreamingSourceDto[];

  @ApiPropertyOptional() @IsString() @IsOptional() imdbId?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() tmdbId?: string;
  @ApiPropertyOptional({ type: [String] }) @IsArray() @IsString({ each: true }) @IsOptional() tags?: string[];
  @ApiPropertyOptional() @IsString() @IsOptional() platformOrigin?: string;
  @ApiPropertyOptional() @IsDate() @Type(() => Date) @IsOptional() scheduledPublishDate?: Date;
  @ApiPropertyOptional() @IsBoolean() @IsOptional() isFeatured?: boolean;
  @ApiPropertyOptional() @IsString() @IsOptional() rankingLabel?: string;
}
