import { Controller, Get, Post, Delete, Body, Param, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { NotificationsService } from './notifications.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@ApiTags('Notifications')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin')
@Controller('notifications')
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  @Get()
  @ApiOperation({ summary: 'Get all notifications (Admin)' })
  async getAll(@Query('page') page?: number, @Query('limit') limit?: number) {
    return this.notificationsService.getAll(page, limit);
  }

  @Post()
  @ApiOperation({ summary: 'Create a notification (Admin)' })
  async create(@Body() body: any, @CurrentUser('userId') createdBy: string) {
    return this.notificationsService.create({ ...body, createdBy });
  }

  @Post(':id/send')
  @ApiOperation({ summary: 'Send a notification (Admin)' })
  async send(@Param('id') id: string) {
    return this.notificationsService.send(id);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a notification (Admin)' })
  async delete(@Param('id') id: string) {
    await this.notificationsService.delete(id);
    return { message: 'Notification deleted' };
  }
}
