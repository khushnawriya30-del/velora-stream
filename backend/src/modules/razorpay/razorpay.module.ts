import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { RazorpayService } from './razorpay.service';
import { RazorpayController } from './razorpay.controller';
import {
  RazorpayPayment,
  RazorpayPaymentSchema,
} from '../../schemas/razorpay-payment.schema';
import { User, UserSchema } from '../../schemas/user.schema';
import {
  PremiumPlanConfig,
  PremiumPlanConfigSchema,
} from '../../schemas/premium-plan.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: RazorpayPayment.name, schema: RazorpayPaymentSchema },
      { name: User.name, schema: UserSchema },
      { name: PremiumPlanConfig.name, schema: PremiumPlanConfigSchema },
    ]),
  ],
  controllers: [RazorpayController],
  providers: [RazorpayService],
  exports: [RazorpayService],
})
export class RazorpayModule {}
