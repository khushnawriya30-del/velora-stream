import { Controller, Get, Post, Patch, Body, Param, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { WithdrawalService } from './withdrawal.service';
import { WithdrawalStatus } from '../../schemas/withdrawal.schema';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('withdraw')
@UseGuards(AuthGuard('jwt'))
export class WithdrawalController {
  constructor(private readonly withdrawalService: WithdrawalService) {}

  @Post()
  async requestWithdrawal(
    @CurrentUser('userId') userId: string,
    @Body() body: {
      amount: number;
      upiId?: string;
      bankName?: string;
      accountNumber?: string;
      ifscCode?: string;
      accountHolderName?: string;
      phoneNumber?: string;
      email?: string;
    },
  ) {
    return this.withdrawalService.requestWithdrawal(userId, body.amount, body.upiId || '', {
      bankName: body.bankName,
      accountNumber: body.accountNumber,
      ifscCode: body.ifscCode,
      accountHolderName: body.accountHolderName,
      phoneNumber: body.phoneNumber,
      email: body.email,
    });
  }

  @Get('history')
  async getHistory(@CurrentUser('userId') userId: string) {
    return this.withdrawalService.getHistory(userId);
  }

  // ── Admin: Withdrawal Management ──

  @Get('admin/all')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async getAdminAll(
    @Query('page') page = 1,
    @Query('limit') limit = 20,
    @Query('status') status?: string,
  ) {
    return this.withdrawalService.getAdminAll(Number(page), Number(limit), status);
  }

  @Patch('admin/:id/approve')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async approveWithdrawal(@Param('id') id: string) {
    return this.withdrawalService.updateStatus(id, WithdrawalStatus.APPROVED);
  }

  @Patch('admin/:id/reject')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async rejectWithdrawal(
    @Param('id') id: string,
    @Body() body: { reason?: string },
  ) {
    return this.withdrawalService.updateStatus(id, WithdrawalStatus.REJECTED, body.reason);
  }
}
