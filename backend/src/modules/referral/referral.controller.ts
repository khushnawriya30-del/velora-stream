import { Controller, Get, Post, Body, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ReferralService } from './referral.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

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
}
