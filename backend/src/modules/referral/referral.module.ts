import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ReferralController } from './referral.controller';
import { ReferralService } from './referral.service';
import { Referral, ReferralSchema } from '../../schemas/referral.schema';
import { User, UserSchema } from '../../schemas/user.schema';
import { PendingReferralVisit, PendingReferralVisitSchema } from '../../schemas/pending-referral-visit.schema';
import { InviteSettings, InviteSettingsSchema } from '../../schemas/invite-settings.schema';
import { WalletModule } from '../wallet/wallet.module';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Referral.name, schema: ReferralSchema },
      { name: User.name, schema: UserSchema },
      { name: PendingReferralVisit.name, schema: PendingReferralVisitSchema },
      { name: InviteSettings.name, schema: InviteSettingsSchema },
    ]),
    WalletModule,
  ],
  controllers: [ReferralController],
  providers: [ReferralService],
  exports: [ReferralService],
})
export class ReferralModule {}
