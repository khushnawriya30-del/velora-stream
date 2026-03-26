import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Review, ReviewDocument, ModerationStatus } from '../../schemas/review.schema';
import { Movie, MovieDocument } from '../../schemas/movie.schema';

@Injectable()
export class ReviewsService {
  constructor(
    @InjectModel(Review.name) private reviewModel: Model<ReviewDocument>,
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
  ) {}

  async createReview(userId: string, contentId: string, rating: number, text?: string): Promise<ReviewDocument> {
    const existing = await this.reviewModel.findOne({
      userId: new Types.ObjectId(userId),
      contentId: new Types.ObjectId(contentId),
    });
    if (existing) throw new ConflictException('You have already reviewed this content');

    const review = await this.reviewModel.create({
      userId: new Types.ObjectId(userId),
      contentId: new Types.ObjectId(contentId),
      rating,
      text,
    });

    await this.recalculateRating(contentId);
    return review;
  }

  async getReviews(contentId: string, page = 1, limit = 20): Promise<{ reviews: ReviewDocument[]; total: number }> {
    const skip = (page - 1) * limit;
    const filter = {
      contentId: new Types.ObjectId(contentId),
      moderationStatus: ModerationStatus.APPROVED,
    };

    const [reviews, total] = await Promise.all([
      this.reviewModel.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit).populate('userId', 'name avatarUrl'),
      this.reviewModel.countDocuments(filter),
    ]);

    return { reviews, total };
  }

  async getAllReviews(page = 1, limit = 50, status?: ModerationStatus): Promise<{ reviews: ReviewDocument[]; total: number }> {
    const skip = (page - 1) * limit;
    const filter: any = {};
    if (status) filter.moderationStatus = status;

    const [reviews, total] = await Promise.all([
      this.reviewModel.find(filter).sort({ createdAt: -1 }).skip(skip).limit(limit)
        .populate('userId', 'name email')
        .populate('contentId', 'title'),
      this.reviewModel.countDocuments(filter),
    ]);

    return { reviews, total };
  }

  async moderateReview(reviewId: string, status: ModerationStatus, moderatorId: string): Promise<ReviewDocument> {
    const review = await this.reviewModel.findByIdAndUpdate(
      reviewId,
      { moderationStatus: status, moderatedBy: moderatorId, moderatedAt: new Date() },
      { new: true },
    );
    if (!review) throw new NotFoundException('Review not found');
    return review;
  }

  async deleteReview(reviewId: string): Promise<void> {
    const review = await this.reviewModel.findByIdAndDelete(reviewId);
    if (!review) throw new NotFoundException('Review not found');
    await this.recalculateRating(review.contentId.toString());
  }

  private async recalculateRating(contentId: string): Promise<void> {
    const result = await this.reviewModel.aggregate([
      { $match: { contentId: new Types.ObjectId(contentId), moderationStatus: ModerationStatus.APPROVED } },
      { $group: { _id: null, avgRating: { $avg: '$rating' }, count: { $sum: 1 } } },
    ]);

    const avgRating = result[0]?.avgRating || 0;
    const count = result[0]?.count || 0;

    await this.movieModel.findByIdAndUpdate(contentId, {
      rating: Math.round(avgRating * 10) / 10,
      voteCount: count,
    });
  }
}
