import { Controller, Get, Post, Body, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { WithdrawalService } from './withdrawal.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@Controller('withdraw')
@UseGuards(AuthGuard('jwt'))
export class WithdrawalController {
  constructor(private readonly withdrawalService: WithdrawalService) {}

  @Post()
  async requestWithdrawal(
    @CurrentUser('_id') userId: string,
    @Body() body: { amount: number; upiId: string },
  ) {
    return this.withdrawalService.requestWithdrawal(userId, body.amount, body.upiId);
  }

  @Get('history')
  async getHistory(@CurrentUser('_id') userId: string) {
    return this.withdrawalService.getHistory(userId);
  }
}
