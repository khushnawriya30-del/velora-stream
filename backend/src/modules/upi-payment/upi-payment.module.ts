import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { UpiPaymentService } from './upi-payment.service';
import { UpiPaymentController } from './upi-payment.controller';
import {
  UpiPayment,
  UpiPaymentSchema,
} from '../../schemas/upi-payment.schema';
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
      { name: UpiPayment.name, schema: UpiPaymentSchema },
      { name: PremiumPlanConfig.name, schema: PremiumPlanConfigSchema },
      { name: User.name, schema: UserSchema },
    ]),
    SettingsModule,
    PremiumModule,
  ],
  controllers: [UpiPaymentController],
  providers: [UpiPaymentService],
  exports: [UpiPaymentService],
})
export class UpiPaymentModule {}
