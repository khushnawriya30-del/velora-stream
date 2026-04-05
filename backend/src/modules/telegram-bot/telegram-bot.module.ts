import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TelegramBotService } from './telegram-bot.service';
import { TelegramBotController } from './telegram-bot.controller';
import {
  TelegramPayment,
  TelegramPaymentSchema,
} from '../../schemas/telegram-payment.schema';
import {
  ActivationCode,
  ActivationCodeSchema,
} from '../../schemas/activation-code.schema';
import {
  PremiumPlanConfig,
  PremiumPlanConfigSchema,
} from '../../schemas/premium-plan.schema';
import { User, UserSchema } from '../../schemas/user.schema';
import { SettingsModule } from '../settings/settings.module';
import { PremiumModule } from '../premium/premium.module';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: TelegramPayment.name, schema: TelegramPaymentSchema },
      { name: ActivationCode.name, schema: ActivationCodeSchema },
      { name: PremiumPlanConfig.name, schema: PremiumPlanConfigSchema },
      { name: User.name, schema: UserSchema },
    ]),
    SettingsModule,
    PremiumModule,
  ],
  controllers: [TelegramBotController],
  providers: [TelegramBotService],
  exports: [TelegramBotService],
})
export class TelegramBotModule {}
