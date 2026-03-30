import { Controller, Get, Put, Body, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AppVersionService } from './app-version.service';
import { AppVersion } from './app-version.schema';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('app-version')
export class AppVersionController {
  constructor(private readonly service: AppVersionService) {}

  @Get()
  getLatest(): Promise<AppVersion> {
    return this.service.getLatest();
  }

  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @Put()
  update(@Body() body: Partial<AppVersion>): Promise<AppVersion> {
    return this.service.update(body);
  }
}
