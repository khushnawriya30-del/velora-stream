import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  UseGuards,
} from '@nestjs/common';
import { PremiumPlansService } from './premium-plans.service';
import { AuthGuard } from '@nestjs/passport';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('premium-plans')
export class PremiumPlansController {
  constructor(private readonly plansService: PremiumPlansService) {}

  // ── Public: fetch active plans ──
  @Get()
  async getPlans() {
    return this.plansService.getActivePlans();
  }

  // ── Admin: fetch all plans (including inactive) ──
  @Get('admin/all')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getAllPlans() {
    return this.plansService.getAllPlans();
  }

  // ── Admin: create plan ──
  @Post('admin')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async createPlan(
    @Body()
    body: {
      planId: string;
      name: string;
      months: number;
      price: number;
      originalPrice: number;
      discountPercent: number;
      badge?: string;
      order?: number;
    },
  ) {
    return this.plansService.createPlan(body);
  }

  // ── Admin: update plan ──
  @Put('admin/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async updatePlan(
    @Param('id') id: string,
    @Body()
    body: Partial<{
      name: string;
      months: number;
      price: number;
      originalPrice: number;
      discountPercent: number;
      badge: string;
      order: number;
      isActive: boolean;
    }>,
  ) {
    return this.plansService.updatePlan(id, body);
  }

  // ── Admin: delete plan ──
  @Delete('admin/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async deletePlan(@Param('id') id: string) {
    return this.plansService.deletePlan(id);
  }

  // ── Admin: seed default plans ──
  @Post('admin/seed')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async seedPlans() {
    return this.plansService.seedDefaultPlans();
  }
}
