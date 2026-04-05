import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import {
  PremiumPlanConfig,
  PremiumPlanConfigDocument,
} from '../../schemas/premium-plan.schema';

const DEFAULT_PLANS = [
  {
    planId: '1m',
    name: '1 Month',
    months: 1,
    price: 159,
    originalPrice: 182,
    discountPercent: 10,
    badge: '',
    order: 0,
    isActive: true,
  },
  {
    planId: '3m',
    name: '3 Months',
    months: 3,
    price: 459,
    originalPrice: 543,
    discountPercent: 15,
    badge: 'Most popular',
    order: 1,
    isActive: true,
  },
  {
    planId: '6m',
    name: '6 Months',
    months: 6,
    price: 829,
    originalPrice: 1038,
    discountPercent: 20,
    badge: '',
    order: 2,
    isActive: true,
  },
  {
    planId: '12m',
    name: '12 Months',
    months: 12,
    price: 1529,
    originalPrice: 2042,
    discountPercent: 25,
    badge: 'Best Value',
    order: 3,
    isActive: true,
  },
];

@Injectable()
export class PremiumPlansService {
  constructor(
    @InjectModel(PremiumPlanConfig.name)
    private planModel: Model<PremiumPlanConfigDocument>,
  ) {}

  async getActivePlans() {
    const plans = await this.planModel
      .find({ isActive: true })
      .sort({ order: 1 })
      .lean();

    // If no plans exist yet, seed defaults and return them
    if (plans.length === 0) {
      await this.seedDefaultPlans();
      return this.planModel
        .find({ isActive: true })
        .sort({ order: 1 })
        .lean();
    }

    return plans;
  }

  async getAllPlans() {
    return this.planModel.find().sort({ order: 1 }).lean();
  }

  async createPlan(data: {
    planId: string;
    name: string;
    months: number;
    price: number;
    originalPrice: number;
    discountPercent: number;
    badge?: string;
    order?: number;
  }) {
    const existing = await this.planModel.findOne({ planId: data.planId });
    if (existing) {
      throw new ConflictException(`Plan with ID "${data.planId}" already exists`);
    }
    return this.planModel.create(data);
  }

  async updatePlan(
    id: string,
    data: Partial<{
      name: string;
      months: number;
      price: number;
      originalPrice: number;
      discountPercent: number;
      badge: string;
      order: number;
      isActive: boolean;
    }>,
  ) {
    const plan = await this.planModel.findByIdAndUpdate(id, data, {
      new: true,
    });
    if (!plan) {
      throw new NotFoundException('Plan not found');
    }
    return plan;
  }

  async deletePlan(id: string) {
    const plan = await this.planModel.findByIdAndDelete(id);
    if (!plan) {
      throw new NotFoundException('Plan not found');
    }
    return { message: 'Plan deleted' };
  }

  async seedDefaultPlans() {
    for (const plan of DEFAULT_PLANS) {
      await this.planModel.updateOne(
        { planId: plan.planId },
        { $setOnInsert: plan },
        { upsert: true },
      );
    }
    return { message: 'Default plans seeded', count: DEFAULT_PLANS.length };
  }
}
