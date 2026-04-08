import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { PremiumOffer, PremiumOfferDocument } from '../../schemas/premium-offer.schema';
import { InviteSettings, InviteSettingsDocument } from '../../schemas/invite-settings.schema';
import { CreatePremiumOfferDto, UpdatePremiumOfferDto, UpdateInviteSettingsDto } from './dto/premium-offers.dto';

@Injectable()
export class PremiumOffersService {
  private readonly logger = new Logger(PremiumOffersService.name);

  constructor(
    @InjectModel(PremiumOffer.name) private offerModel: Model<PremiumOfferDocument>,
    @InjectModel(InviteSettings.name) private inviteModel: Model<InviteSettingsDocument>,
  ) {}

  // ── Premium Offers CRUD ──

  async createOffer(dto: CreatePremiumOfferDto): Promise<PremiumOfferDocument> {
    const discountPercent = dto.originalPrice > 0
      ? Math.round(((dto.originalPrice - dto.discountPrice) / dto.originalPrice) * 100)
      : 0;
    const offer = await this.offerModel.create({ ...dto, discountPercent });
    this.logger.log(`Created premium offer: ${offer.title}`);
    return offer;
  }

  async updateOffer(id: string, dto: UpdatePremiumOfferDto): Promise<PremiumOfferDocument> {
    const update: any = { ...dto };
    if (dto.originalPrice !== undefined && dto.discountPrice !== undefined) {
      update.discountPercent = dto.originalPrice > 0
        ? Math.round(((dto.originalPrice - dto.discountPrice) / dto.originalPrice) * 100)
        : 0;
    }
    const offer = await this.offerModel.findByIdAndUpdate(id, { $set: update }, { new: true });
    if (!offer) throw new NotFoundException('Offer not found');
    return offer;
  }

  async deleteOffer(id: string): Promise<void> {
    const result = await this.offerModel.findByIdAndDelete(id);
    if (!result) throw new NotFoundException('Offer not found');
  }

  async getAllOffers(): Promise<PremiumOfferDocument[]> {
    return this.offerModel.find().sort({ order: 1, createdAt: -1 });
  }

  async getOffersForUser(isPremium: boolean): Promise<PremiumOfferDocument[]> {
    const targetType = isPremium ? 'premium' : 'non_premium';
    const now = new Date();
    return this.offerModel.find({
      isVisible: true,
      targetUserType: { $in: [targetType, 'all'] },
      $or: [
        { startDate: null, endDate: null },
        { startDate: { $lte: now }, endDate: null },
        { startDate: null, endDate: { $gte: now } },
        { startDate: { $lte: now }, endDate: { $gte: now } },
      ],
    }).sort({ order: 1 });
  }

  async getPopupOffers(isPremium: boolean): Promise<PremiumOfferDocument[]> {
    const targetType = isPremium ? 'premium' : 'non_premium';
    const now = new Date();
    return this.offerModel.find({
      isVisible: true,
      showAsPopup: true,
      targetUserType: { $in: [targetType, 'all'] },
      $or: [
        { startDate: null, endDate: null },
        { startDate: { $lte: now }, endDate: null },
        { startDate: null, endDate: { $gte: now } },
        { startDate: { $lte: now }, endDate: { $gte: now } },
      ],
    }).sort({ order: 1 });
  }

  // ── Invite Settings ──

  async getInviteSettings(): Promise<InviteSettingsDocument> {
    let settings = await this.inviteModel.findOne({ key: 'default' });
    if (!settings) {
      settings = await this.inviteModel.create({
        key: 'default',
        targetAmount: 100,
        defaultBalance: 80,
        rewardPerInvite: 1,
        earnWindowDays: 60,
        isActive: true,
      });
    }
    return settings;
  }

  async updateInviteSettings(dto: UpdateInviteSettingsDto): Promise<InviteSettingsDocument> {
    let settings = await this.inviteModel.findOne({ key: 'default' });
    if (!settings) {
      settings = await this.inviteModel.create({ key: 'default', ...dto });
    } else {
      Object.assign(settings, dto);
      await settings.save();
    }
    this.logger.log(`Updated invite settings: ${JSON.stringify(dto)}`);
    return settings;
  }
}
