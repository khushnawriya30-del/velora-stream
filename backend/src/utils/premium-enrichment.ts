import { Model } from 'mongoose';

/**
 * Enriches an array of movies with `hasPremiumEpisode: true` for any series
 * that has at least one episode marked as premium.
 * Uses Mongoose models via the connection to avoid native driver type issues.
 */
export async function enrichWithPremiumEpisodeFlag(
  movieModel: Model<any>,
  movies: any[],
): Promise<any[]> {
  if (!movies || movies.length === 0) return movies;

  const seriesTypes = ['series', 'web_series', 'tv_show', 'anime'];
  const seriesMovies = movies.filter((m) => {
    const ct = m.toObject ? m.toObject().contentType : m.contentType;
    return seriesTypes.includes(ct);
  });
  if (seriesMovies.length === 0) return movies;

  const seriesIds = seriesMovies.map((m) => m._id);

  // Use Mongoose models registered on the same connection
  const conn = movieModel.db;
  const SeasonModel = conn.model('Season');
  const EpisodeModel = conn.model('Episode');

  try {
    // 1) Get all seasons for these series
    const seasons: any[] = await SeasonModel
      .find({ seriesId: { $in: seriesIds } })
      .select('_id seriesId')
      .lean();
    if (seasons.length === 0) return movies.map((m) => m.toObject ? m.toObject() : { ...m });

    // Map seasonId → seriesId
    const seasonToSeries = new Map<string, string>();
    for (const s of seasons) {
      seasonToSeries.set(s._id.toString(), s.seriesId.toString());
    }

    // 2) Find any premium episodes in those seasons
    const premiumEps: any[] = await EpisodeModel
      .find({ seasonId: { $in: seasons.map((s: any) => s._id) }, isPremium: true })
      .select('seasonId')
      .lean();

    const premiumSeriesIds = new Set<string>();
    for (const ep of premiumEps) {
      const sid = seasonToSeries.get(ep.seasonId.toString());
      if (sid) premiumSeriesIds.add(sid);
    }

    // 3) Add hasPremiumEpisode flag to matching series
    return movies.map((m) => {
      const obj = m.toObject ? m.toObject() : { ...m };
      if (premiumSeriesIds.has(obj._id.toString())) {
        obj.hasPremiumEpisode = true;
      }
      return obj;
    });
  } catch (err) {
    console.error('enrichWithPremiumEpisodeFlag error:', err);
    return movies.map((m) => m.toObject ? m.toObject() : { ...m });
  }
}
