import { Controller, Get, Post, Body, Query, Param, UseGuards, Req, Logger } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Request } from 'express';
import { ReferralService } from './referral.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('referral')
export class ReferralController {
  private readonly logger = new Logger(ReferralController.name);
  constructor(private readonly referralService: ReferralService) {}

  private getClientIp(req: Request): string {
    return (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim()
      || req.socket?.remoteAddress
      || 'unknown';
  }

  // ── Public endpoints (no auth) — called by website & app ──

  @Post('track-visit')
  async trackVisit(@Body() body: { referralCode: string }, @Req() req: Request) {
    const ip = this.getClientIp(req);
    const userAgent = req.headers['user-agent'] || '';
    this.logger.log(`track-visit called: code=${body.referralCode}, ip=${ip}, ua=${userAgent?.substring(0, 50)}`);
    await this.referralService.trackVisit(body.referralCode, ip, userAgent);
    return { success: true, ip };
  }

  @Get('check-pending')
  async checkPending(@Req() req: Request) {
    const ip = this.getClientIp(req);
    this.logger.log(`check-pending called: ip=${ip}`);
    const referralCode = await this.referralService.findReferralCodeByIp(ip, false);
    this.logger.log(`check-pending result: ip=${ip}, code=${referralCode || 'none'}`);
    return { referralCode: referralCode || null };
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
    this.logger.log(`POST /referral/apply called: userId=${userId}, referralCode=${body.referralCode}`);
    await this.referralService.applyReferral(userId, body.referralCode);
    this.logger.log(`POST /referral/apply success for userId=${userId}`);
    return { success: true, message: 'Referral applied successfully' };
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
