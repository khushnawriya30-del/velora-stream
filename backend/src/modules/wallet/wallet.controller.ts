import { Controller, Get, Post, Body, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { WalletService } from './wallet.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('wallet')
@UseGuards(AuthGuard('jwt'))
export class WalletController {
  constructor(private readonly walletService: WalletService) {}

  @Get('balance')
  async getBalance(@CurrentUser('userId') userId: string) {
    return this.walletService.getBalance(userId);
  }

  @Get('bank-details')
  async getBankDetails(@CurrentUser('userId') userId: string) {
    return this.walletService.getBankDetails(userId);
  }

  @Post('bank-details')
  async saveBankDetails(
    @CurrentUser('userId') userId: string,
    @Body() body: {
      bankName: string;
      accountNumber: string;
      ifscCode: string;
      accountHolderName: string;
      phoneNumber: string;
      email: string;
    },
  ) {
    return this.walletService.saveBankDetails(userId, {
      bankName: body.bankName,
      accountNumber: body.accountNumber,
      ifscCode: body.ifscCode,
      accountHolderName: body.accountHolderName,
      phoneNumber: body.phoneNumber,
      bankEmail: body.email,
    });
  }

  @Get('admin/user/:userId/bank-details')
  @UseGuards(RolesGuard)
  @Roles('admin')
  async getAdminUserBankDetails(@Param('userId') userId: string) {
    return this.walletService.getAdminUserBankDetails(userId);
  }
}
