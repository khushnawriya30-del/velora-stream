import { Controller, Post, Body, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { GdriveFolderService, ScanResult } from './gdrive-folder.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';

@ApiTags('Google Drive Folder')
@Controller('gdrive-folder')
export class GdriveFolderController {
  constructor(private readonly gdriveFolderService: GdriveFolderService) {}

  @Post('scan')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Scan a public Google Drive folder and detect episodes (Admin)' })
  async scanFolder(@Body() body: { folderUrl: string }) {
    return this.gdriveFolderService.scanFolder(body.folderUrl);
  }

  @Post('import')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Import scanned episodes into a series (Admin)' })
  async importToSeries(
    @Body() body: { seriesId: string; scanResult: ScanResult },
  ) {
    return this.gdriveFolderService.importToSeries(body.seriesId, body.scanResult);
  }
}
