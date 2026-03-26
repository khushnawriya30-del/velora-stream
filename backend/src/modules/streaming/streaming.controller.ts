import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { StreamingService } from './streaming.service';

@ApiTags('Streaming')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'))
@Controller('streaming')
export class StreamingController {
  constructor(private readonly streamingService: StreamingService) {}

  @Get('url')
  @ApiOperation({ summary: 'Get signed streaming URL' })
  async getStreamUrl(@Query('path') path: string) {
    return this.streamingService.getSignedStreamUrl(path);
  }
}
