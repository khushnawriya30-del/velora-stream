import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';

@ApiTags('Root')
@Controller()
export class AppController {
  @Get()
  @ApiOperation({ summary: 'API Welcome' })
  root() {
    return {
      message: 'VELORA API',
      version: '1.0.0',
      status: 'running',
      apiBaseUrl: '/api/v1',
      documentation: '/docs',
      endpoints: {
        welcome: '/api/v1',
        health: '/api/v1/health',
        homeFeed: '/api/v1/home/feed',
        banners: '/api/v1/banners',
      },
    };
  }
}
