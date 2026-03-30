import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';

@ApiTags('Health')
@Controller()
export class HealthController {
  @Get('health')
  @ApiOperation({ summary: 'Health check endpoint' })
  check() {
    return {
      status: 'ok',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
    };
  }

  @Get()
  @ApiOperation({ summary: 'API Welcome message' })
  welcome() {
    return {
      message: 'VELORA API Running',
      version: '1.0.0',
      status: 'ok',
      endpoints: {
        health: '/api/v1/health',
        homeFeed: '/api/v1/home/feed',
        banners: '/api/v1/banners',
        movies: '/api/v1/movies',
        docs: '/docs',
      },
      timestamp: new Date().toISOString(),
    };
  }
}
