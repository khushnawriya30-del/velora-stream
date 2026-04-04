import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  Param,
  UseGuards,
  Req,
} from '@nestjs/common';
import { PremiumService } from './premium.service';
import { AuthGuard } from '@nestjs/passport';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { PremiumPlan } from '../../schemas/activation-code.schema';

@Controller('premium')
export class PremiumController {
  constructor(private readonly premiumService: PremiumService) {}

  // ════════════════════════════════════════════
  //  USER ENDPOINTS (Authenticated)
  // ════════════════════════════════════════════

  // POST /premium/activate { code: "VLRA-XXXX-XXXX" }
  @Post('activate')
  @UseGuards(AuthGuard('jwt'))
  async activateCode(@Req() req: any, @Body() body: { code: string }) {
    return this.premiumService.activateCode(req.user.userId, body.code);
  }

  // GET /premium/status
  @Get('status')
  @UseGuards(AuthGuard('jwt'))
  async getStatus(@Req() req: any) {
    return this.premiumService.getPremiumStatus(req.user.userId);
  }

  // ════════════════════════════════════════════
  //  ADMIN ENDPOINTS
  // ════════════════════════════════════════════

  // POST /premium/admin/generate { plan, count, note? }
  @Post('admin/generate')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async generateCodes(
    @Body()
    body: {
      plan: PremiumPlan;
      count: number;
      note?: string;
      expiresInDays?: number;
    },
  ) {
    return this.premiumService.generateCodes(body.plan, body.count, {
      note: body.note,
      expiresInDays: body.expiresInDays,
    });
  }

  // GET /premium/admin/codes?plan=1month&isRedeemed=false&page=1&limit=50
  @Get('admin/codes')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async listCodes(
    @Query('plan') plan?: string,
    @Query('isRedeemed') isRedeemed?: string,
    @Query('batchId') batchId?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.premiumService.listCodes({
      plan,
      isRedeemed: isRedeemed !== undefined ? isRedeemed === 'true' : undefined,
      batchId,
      page: page ? parseInt(page) : 1,
      limit: limit ? parseInt(limit) : 50,
    });
  }

  // POST /premium/admin/revoke/:id { reason? }
  @Post('admin/revoke/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async revokeCode(
    @Param('id') id: string,
    @Body() body: { reason?: string },
  ) {
    return this.premiumService.revokeCode(id, body.reason);
  }

  // GET /premium/admin/users?page=1&limit=50
  @Get('admin/users')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async listPremiumUsers(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.premiumService.listPremiumUsers(
      page ? parseInt(page) : 1,
      limit ? parseInt(limit) : 50,
    );
  }

  // GET /premium/admin/stats
  @Get('admin/stats')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getStats() {
    return this.premiumService.getStats();
  }
}
