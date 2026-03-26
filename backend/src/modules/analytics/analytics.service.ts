import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { User, UserDocument } from '../../schemas/user.schema';
import { Movie, MovieDocument } from '../../schemas/movie.schema';
import { WatchProgress, WatchProgressDocument } from '../../schemas/watch-progress.schema';

@Injectable()
export class AnalyticsService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
    @InjectModel(WatchProgress.name) private progressModel: Model<WatchProgressDocument>,
  ) {}

  async getDashboard(): Promise<any> {
    const now = new Date();
    const dayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    const [
      totalUsers,
      newUsersToday,
      newUsersMonth,
      dau,
      mau,
      totalContent,
      topWatched,
    ] = await Promise.all([
      this.userModel.countDocuments(),
      this.userModel.countDocuments({ createdAt: { $gte: dayAgo } }),
      this.userModel.countDocuments({ createdAt: { $gte: monthAgo } }),
      this.userModel.countDocuments({ lastActiveAt: { $gte: dayAgo } }),
      this.userModel.countDocuments({ lastActiveAt: { $gte: monthAgo } }),
      this.movieModel.countDocuments(),
      this.movieModel.find().sort({ viewCount: -1 }).limit(10)
        .select('title viewCount rating posterUrl contentType'),
    ]);

    return {
      users: { total: totalUsers, newToday: newUsersToday, newThisMonth: newUsersMonth, dau, mau },
      content: { total: totalContent },
      topWatched,
    };
  }

  async getUserSignups(days = 30): Promise<any[]> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    return this.userModel.aggregate([
      { $match: { createdAt: { $gte: startDate } } },
      {
        $group: {
          _id: { $dateToString: { format: '%Y-%m-%d', date: '$createdAt' } },
          count: { $sum: 1 },
        },
      },
      { $sort: { _id: 1 } },
    ]);
  }

  async getMostWatched(limit = 20): Promise<any[]> {
    return this.movieModel
      .find({ status: 'published' })
      .sort({ viewCount: -1 })
      .limit(limit)
      .select('title viewCount rating posterUrl contentType releaseYear');
  }
}
