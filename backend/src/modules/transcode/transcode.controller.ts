import { Controller, Post, Get, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { TranscodeService } from './transcode.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';

@ApiTags('Transcode')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin', 'content_manager')
@Controller('transcode')
export class TranscodeController {
  constructor(private readonly transcodeService: TranscodeService) {}

  @Post(':movieId')
  @ApiOperation({ summary: 'Start HLS transcoding for a movie (Admin)' })
  async startTranscode(@Param('movieId') movieId: string) {
    return this.transcodeService.startTranscode(movieId);
  }

  @Get(':movieId/status')
  @ApiOperation({ summary: 'Get transcoding status (Admin)' })
  async getStatus(@Param('movieId') movieId: string) {
    return this.transcodeService.getStatus(movieId);
  }
}
