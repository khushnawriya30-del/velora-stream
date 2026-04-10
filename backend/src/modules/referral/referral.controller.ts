import { Controller, Get, Post, Body, Query, Param, UseGuards, Req } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Request } from 'express';
import { ReferralService } from './referral.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('referral')
export class ReferralController {
  constructor(private readonly referralService: ReferralService) {}

  // ── Public endpoint (no auth) — called by website ──

  @Post('track-visit')
  async trackVisit(@Body() body: { referralCode: string }, @Req() req: Request) {
    const ip = (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim()
      || req.socket?.remoteAddress
      || 'unknown';
    const userAgent = req.headers['user-agent'] || '';
    await this.referralService.trackVisit(body.referralCode, ip, userAgent);
    return { success: true };
  }

  // ── Authenticated endpoints ──

  @Get('stats')
  @UseGuards(AuthGuard('jwt'))
  async getStats(@CurrentUser('userId') userId: string) {
    return this.referralService.getReferralStats(userId);
  }

  @Get('earnings')
  @UseGuards(AuthGuard('jwt'))
  async getEarnings(@CurrentUser('userId') userId: string) {
    return this.referralService.getEarningsHistory(userId);
  }

  @Post('apply')
  @UseGuards(AuthGuard('jwt'))
  async applyReferral(
    @CurrentUser('userId') userId: string,
    @Body() body: { referralCode: string },
  ) {
    await this.referralService.applyReferral(userId, body.referralCode);
    return { message: 'Referral applied successfully' };
  }

  // ── Admin: Referral Dashboard ──

  @Get('admin/dashboard')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getAdminDashboard(
    @Query('page') page = 1,
    @Query('limit') limit = 20,
    @Query('search') search?: string,
  ) {
    return this.referralService.getAdminDashboard(Number(page), Number(limit), search);
  }

  @Get('admin/user/:userId')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getAdminUserReferrals(@Param('userId') userId: string) {
    return this.referralService.getAdminUserReferrals(userId);
  }
}
