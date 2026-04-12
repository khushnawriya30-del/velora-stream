import { Model, Connection } from 'mongoose';

/**
 * Enriches an array of movies with `hasPremiumEpisode: true` for any series
 * that has at least one episode marked as premium.
 * Uses raw MongoDB queries via the model's db connection (no extra DI needed).
 * Performs exactly 2 queries regardless of movie count.
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

  // Access the native MongoDB Db via Mongoose connection
  const conn: Connection = movieModel.db;
  const nativeDb = conn.getClient().db();

  try {
    // 1) Get all seasons for these series
    const seasons = await nativeDb
      .collection('seasons')
      .find({ seriesId: { $in: seriesIds } }, { projection: { _id: 1, seriesId: 1 } })
      .toArray();
    if (seasons.length === 0) return movies.map((m) => m.toObject ? m.toObject() : { ...m });

    // Map seasonId → seriesId
    const seasonToSeries = new Map<string, string>();
    for (const s of seasons) {
      seasonToSeries.set(s._id.toString(), s.seriesId.toString());
    }

    // 2) Find any premium episodes in those seasons
    const premiumEps = await nativeDb
      .collection('episodes')
      .find(
        { seasonId: { $in: seasons.map((s) => s._id) }, isPremium: true },
        { projection: { seasonId: 1 } },
      )
      .toArray();

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
    // On any error, return movies as plain objects without enrichment
    console.error('enrichWithPremiumEpisodeFlag error:', err);
    return movies.map((m) => m.toObject ? m.toObject() : { ...m });
  }
}
