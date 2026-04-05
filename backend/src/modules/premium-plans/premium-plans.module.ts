import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { PremiumPlansService } from './premium-plans.service';
import { PremiumPlansController } from './premium-plans.controller';
import {
  PremiumPlanConfig,
  PremiumPlanConfigSchema,
} from '../../schemas/premium-plan.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: PremiumPlanConfig.name, schema: PremiumPlanConfigSchema },
    ]),
  ],
  controllers: [PremiumPlansController],
  providers: [PremiumPlansService],
  exports: [PremiumPlansService],
})
export class PremiumPlansModule {}
