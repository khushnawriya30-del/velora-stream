import { Controller, Get, Post, Body, Query, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ReferralService } from './referral.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('referral')
@UseGuards(AuthGuard('jwt'))
export class ReferralController {
  constructor(private readonly referralService: ReferralService) {}

  @Get('stats')
  async getStats(@CurrentUser('_id') userId: string) {
    return this.referralService.getReferralStats(userId);
  }

  @Get('earnings')
  async getEarnings(@CurrentUser('_id') userId: string) {
    return this.referralService.getEarningsHistory(userId);
  }

  @Post('apply')
  async applyReferral(
    @CurrentUser('_id') userId: string,
    @Body() body: { referralCode: string },
  ) {
    await this.referralService.applyReferral(userId, body.referralCode);
    return { message: 'Referral applied successfully' };
  }

  // ── Admin: Referral Dashboard ──

  @Get('admin/dashboard')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async getAdminDashboard(
    @Query('page') page = 1,
    @Query('limit') limit = 20,
    @Query('search') search?: string,
  ) {
    return this.referralService.getAdminDashboard(Number(page), Number(limit), search);
  }

  @Get('admin/user/:userId')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async getAdminUserReferrals(@Param('userId') userId: string) {
    return this.referralService.getAdminUserReferrals(userId);
  }
}
