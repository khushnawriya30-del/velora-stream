import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { PremiumOffer, PremiumOfferSchema } from '../../schemas/premium-offer.schema';
import { InviteSettings, InviteSettingsSchema } from '../../schemas/invite-settings.schema';
import { PremiumOffersService } from './premium-offers.service';
import { PremiumOffersController } from './premium-offers.controller';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: PremiumOffer.name, schema: PremiumOfferSchema },
      { name: InviteSettings.name, schema: InviteSettingsSchema },
    ]),
  ],
  controllers: [PremiumOffersController],
  providers: [PremiumOffersService],
  exports: [PremiumOffersService],
})
export class PremiumOffersModule {}
