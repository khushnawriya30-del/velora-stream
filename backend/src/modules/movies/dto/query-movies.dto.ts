import { IsOptional, IsString, IsNumber, IsEnum } from 'class-validator';
import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { ContentType, ContentStatus } from '../../../schemas/movie.schema';

export class QueryMoviesDto {
  @ApiPropertyOptional() @IsNumber() @Type(() => Number) @IsOptional() page?: number;
  @ApiPropertyOptional() @IsNumber() @Type(() => Number) @IsOptional() limit?: number;

  @ApiPropertyOptional({ enum: ContentType })
  @IsEnum(ContentType) @IsOptional()
  contentType?: ContentType;

  @ApiPropertyOptional() @IsString() @IsOptional() genre?: string;
  @ApiPropertyOptional() @IsString() @IsOptional() language?: string;
  @ApiPropertyOptional() @IsNumber() @Type(() => Number) @IsOptional() year?: number;
  @ApiPropertyOptional() @IsNumber() @Type(() => Number) @IsOptional() rating?: number;
  @ApiPropertyOptional() @IsString() @IsOptional() sort?: string;

  @ApiPropertyOptional({ enum: ContentStatus })
  @IsEnum(ContentStatus) @IsOptional()
  status?: ContentStatus;
}
