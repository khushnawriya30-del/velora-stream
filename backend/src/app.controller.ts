import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';

// Bump this whenever you release a new APK to GitHub Releases
const LATEST_VERSION_CODE = 1;
const LATEST_VERSION_NAME = '1.0.0';
const APK_DOWNLOAD_URL =
  'https://github.com/vishu09921202023-ops/Cinevault-App/releases/latest/download/app-release.apk';

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

  @Get('app-version')
  @ApiOperation({ summary: 'Get latest Android app version info' })
  getAppVersion() {
    return {
      versionCode: LATEST_VERSION_CODE,
      versionName: LATEST_VERSION_NAME,
      forceUpdate: false,
      apkUrl: APK_DOWNLOAD_URL,
      releaseNotes: 'Latest version of VELORA',
    };
  }
}
