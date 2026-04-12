import { Controller, Get, Put, Body, Query, Res, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Response } from 'express';
import { AppVersionService } from './app-version.service';
import { AppVersion } from './app-version.schema';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('app-version')
export class AppVersionController {
  constructor(private readonly service: AppVersionService) {}

  @Get()
  getLatest(@Query('platform') platform?: string): Promise<AppVersion> {
    return this.service.getLatest(platform || 'mobile');
  }

  /**
   * Redirect endpoint — resolves GitHub Releases 302 → direct CDN URL.
   * DownloadManager on many Android devices chokes on GitHub's redirect chain
   * (long SAS query strings, missing Content-Length). This endpoint does a HEAD
   * request server-side, follows the redirect, and returns a clean 302 to the
   * final CDN URL that DownloadManager can handle.
   */
  @Get('download')
  async download(@Query('platform') platform: string, @Res() res: Response) {
    const resolved = await this.service.resolveDownloadUrl(platform || 'mobile');
    res.redirect(302, resolved);
  }

  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @Put()
  update(@Body() body: Partial<AppVersion>): Promise<AppVersion> {
    return this.service.update(body);
  }
}
