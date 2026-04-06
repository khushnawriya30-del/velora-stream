import {
  Controller,
  Get,
  Post,
  Param,
  Query,
  Body,
  UseGuards,
  HttpCode,
  ForbiddenException,
  Req,
} from '@nestjs/common';
import { TelegramBotService } from './telegram-bot.service';
import { AuthGuard } from '@nestjs/passport';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('telegram-bot')
export class TelegramBotController {
  constructor(private readonly botService: TelegramBotService) {}

  // ════════════════════════════════════════════
  //  WEBHOOK ENDPOINT (called by Telegram)
  // ════════════════════════════════════════════

  @Post('webhook/:secret')
  @HttpCode(200)
  async handleWebhook(
    @Param('secret') secret: string,
    @Body() update: any,
  ) {
    if (secret !== this.botService.getWebhookSecret()) {
      throw new ForbiddenException();
    }
    await this.botService.handleWebhookUpdate(update);
    return { ok: true };
  }

  // ════════════════════════════════════════════
  //  ADMIN ENDPOINTS
  // ════════════════════════════════════════════

  // GET /telegram-bot/admin/payments?status=verified&page=1&limit=50
  @Get('admin/payments')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getPayments(
    @Query('status') status?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.botService.getPayments({
      status,
      page: page ? parseInt(page) : 1,
      limit: limit ? parseInt(limit) : 50,
    });
  }

  // GET /telegram-bot/admin/stats
  @Get('admin/stats')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getStats() {
    return this.botService.getPaymentStats();
  }

  // POST /telegram-bot/admin/reject/:id { reason? }
  @Post('admin/reject/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async rejectPayment(
    @Param('id') id: string,
    @Body() body: { reason?: string },
  ) {
    return this.botService.rejectPayment(id, body.reason);
  }

  // POST /telegram-bot/admin/verify/:id
  @Post('admin/verify/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async manualVerify(@Param('id') id: string) {
    return this.botService.manualVerifyPayment(id);
  }

  // GET /telegram-bot/admin/status
  @Get('admin/status')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getBotStatus() {
    return this.botService.getBotStatus();
  }

  // POST /telegram-bot/admin/restart
  @Post('admin/restart')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async restartBot() {
    await this.botService.restartBot();
    return { success: true, message: 'Bot restarted' };
  }
}
