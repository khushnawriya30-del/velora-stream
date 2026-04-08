import { Controller, Get, Post, Put, Delete, Body, Param, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { PremiumOffersService } from './premium-offers.service';
import { CreatePremiumOfferDto, UpdatePremiumOfferDto, UpdateInviteSettingsDto } from './dto/premium-offers.dto';

// Simple admin role guard
const AdminGuard = () => UseGuards(AuthGuard('jwt'));

@Controller('premium-offers')
export class PremiumOffersController {
  constructor(private readonly service: PremiumOffersService) {}

  // ── Public: Get offers for a user ──
  @Get()
  async getOffers(@Query('isPremium') isPremium: string) {
    const premium = isPremium === 'true';
    return this.service.getOffersForUser(premium);
  }

  @Get('popup')
  async getPopupOffers(@Query('isPremium') isPremium: string) {
    const premium = isPremium === 'true';
    return this.service.getPopupOffers(premium);
  }

  // ── Public: Get invite settings ──
  @Get('invite-settings')
  async getInviteSettings() {
    return this.service.getInviteSettings();
  }

  // ── Admin: CRUD offers ──
  @AdminGuard()
  @Get('admin/all')
  async getAllOffers() {
    return this.service.getAllOffers();
  }

  @AdminGuard()
  @Post('admin')
  async createOffer(@Body() dto: CreatePremiumOfferDto) {
    return this.service.createOffer(dto);
  }

  // ── Admin: Invite Settings (must be BEFORE admin/:id to avoid route conflict) ──
  @AdminGuard()
  @Put('admin/invite-settings')
  async updateInviteSettings(@Body() dto: UpdateInviteSettingsDto) {
    return this.service.updateInviteSettings(dto);
  }

  @AdminGuard()
  @Put('admin/:id')
  async updateOffer(@Param('id') id: string, @Body() dto: UpdatePremiumOfferDto) {
    return this.service.updateOffer(id, dto);
  }

  @AdminGuard()
  @Delete('admin/:id')
  async deleteOffer(@Param('id') id: string) {
    await this.service.deleteOffer(id);
    return { message: 'Offer deleted' };
  }
}
