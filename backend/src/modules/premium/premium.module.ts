import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { PremiumService } from './premium.service';
import { PremiumController } from './premium.controller';
import { User, UserSchema } from '../../schemas/user.schema';
import {
  ActivationCode,
  ActivationCodeSchema,
} from '../../schemas/activation-code.schema';
import { ScheduleModule } from '@nestjs/schedule';
import { PremiumCronService } from './premium-cron.service';

@Module({
  imports: [
    ScheduleModule.forRoot(),
    MongooseModule.forFeature([
      { name: User.name, schema: UserSchema },
      { name: ActivationCode.name, schema: ActivationCodeSchema },
    ]),
  ],
  controllers: [PremiumController],
  providers: [PremiumService, PremiumCronService],
  exports: [PremiumService],
})
export class PremiumModule {}
