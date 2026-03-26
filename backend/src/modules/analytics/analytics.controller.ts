import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { AnalyticsService } from './analytics.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';

@ApiTags('Analytics')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin')
@Controller('analytics')
export class AnalyticsController {
  constructor(private readonly analyticsService: AnalyticsService) {}

  @Get('dashboard')
  @ApiOperation({ summary: 'Get analytics dashboard (Admin)' })
  async getDashboard() {
    return this.analyticsService.getDashboard();
  }

  @Get('signups')
  @ApiOperation({ summary: 'Get user signup chart data (Admin)' })
  async getSignups(@Query('days') days?: number) {
    return this.analyticsService.getUserSignups(days);
  }

  @Get('most-watched')
  @ApiOperation({ summary: 'Get most watched content (Admin)' })
  async getMostWatched(@Query('limit') limit?: number) {
    return this.analyticsService.getMostWatched(limit);
  }
}
