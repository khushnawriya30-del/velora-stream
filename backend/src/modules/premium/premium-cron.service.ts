import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { PremiumService } from './premium.service';

@Injectable()
export class PremiumCronService {
  private readonly logger = new Logger(PremiumCronService.name);

  constructor(private readonly premiumService: PremiumService) {}

  // Run every day at 2:00 AM
  @Cron(CronExpression.EVERY_DAY_AT_2AM)
  async handleExpiredSubscriptions() {
    this.logger.log('Checking for expired premium subscriptions...');
    const result = await this.premiumService.expireSubscriptions();
    this.logger.log(`Expired ${result.expiredCount} subscriptions`);
  }
}
