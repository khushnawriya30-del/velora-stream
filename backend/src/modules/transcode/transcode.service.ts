import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { Movie, MovieDocument } from '../../schemas/movie.schema';
import { TranscoderServiceClient } from '@google-cloud/video-transcoder';
import { Storage } from '@google-cloud/storage';
import * as http from 'http';
import * as https from 'https';
import * as path from 'path';

@Injectable()
export class TranscodeService {
  private readonly logger = new Logger(TranscodeService.name);
  private readonly transcoderClient: TranscoderServiceClient;
  private readonly storage: Storage;
  private readonly projectId: string;
  private readonly location: string;
  private readonly sourceBucket: string;
  private readonly outputBucket: string;

  constructor(
    @InjectModel(Movie.name) private movieModel: Model<MovieDocument>,
    private configService: ConfigService,
  ) {
    const keyFilePath = path.join(process.cwd(), 'gcp-service-account.json');
    this.projectId = this.configService.get<string>('GCP_PROJECT_ID', 'cinevault-streaming');
    this.location = this.configService.get<string>('GCP_LOCATION', 'asia-south1');
    this.sourceBucket = this.configService.get<string>('GCP_SOURCE_BUCKET', 'cinevault-source');
    this.outputBucket = this.configService.get<string>('GCP_OUTPUT_BUCKET', 'cinevault-hls-output');

    this.transcoderClient = new TranscoderServiceClient({ keyFilename: keyFilePath });
    this.storage = new Storage({ keyFilename: keyFilePath });
  }

  async startTranscode(movieId: string): Promise<{ status: string; message: string }> {
    const movie = await this.movieModel.findById(movieId);
    if (!movie) throw new NotFoundException('Movie not found');

    const sourceUrl = movie.streamingSources?.[0]?.url;
    if (!sourceUrl) throw new NotFoundException('No streaming source URL found');

    if (movie.hlsStatus === 'processing') {
      return { status: 'processing', message: 'Transcoding already in progress' };
    }

    await this.movieModel.findByIdAndUpdate(movieId, { hlsStatus: 'processing' }, { runValidators: false });

    this.processTranscode(movieId, sourceUrl).catch((error) => {
      this.logger.error(`Transcoding failed for ${movieId}: ${error.message}`);
      this.movieModel.findByIdAndUpdate(movieId, { hlsStatus: 'failed' }, { runValidators: false }).exec();
    });

    return { status: 'processing', message: 'Transcoding started on Google Cloud. This is much faster than local encoding.' };
  }

  async getStatus(movieId: string): Promise<{ status: string; hlsUrl?: string }> {
    const movie = await this.movieModel.findById(movieId);
    if (!movie) throw new NotFoundException('Movie not found');
    return {
      status: movie.hlsStatus || 'none',
      hlsUrl: movie.hlsUrl || undefined,
    };
  }

  private async processTranscode(movieId: string, sourceUrl: string): Promise<void> {
    // Step 1: Upload source to GCS (stream from URL directly to bucket)
    const gcsSourcePath = `sources/${movieId}/${this.getFilenameFromUrl(sourceUrl)}`;
    this.logger.log(`[${movieId}] Uploading source to GCS: gs://${this.sourceBucket}/${gcsSourcePath}`);
    await this.uploadUrlToGcs(sourceUrl, this.sourceBucket, gcsSourcePath);
    this.logger.log(`[${movieId}] Upload to GCS complete`);

    // Step 2: Create transcoding job with HLS output (360p, 480p, 720p, 1080p)
    const outputPrefix = `hls/${movieId}/`;
    const inputUri = `gs://${this.sourceBucket}/${gcsSourcePath}`;
    const outputUri = `gs://${this.outputBucket}/${outputPrefix}`;

    this.logger.log(`[${movieId}] Creating Google Cloud Transcoder job...`);

    const [job] = await this.transcoderClient.createJob({
      parent: this.transcoderClient.locationPath(this.projectId, this.location),
      job: {
        inputUri,
        outputUri,
        config: {
          elementaryStreams: [
            // Video streams at different resolutions
            {
              key: 'video-1080p',
              videoStream: {
                h264: {
                  heightPixels: 1080,
                  widthPixels: 1920,
                  bitrateBps: 5000000,
                  frameRate: 24,
                  gopDuration: { seconds: 2 },
                  profile: 'high',
                  preset: 'veryfast',
                },
              },
            },
            {
              key: 'video-720p',
              videoStream: {
                h264: {
                  heightPixels: 720,
                  widthPixels: 1280,
                  bitrateBps: 3000000,
                  frameRate: 24,
                  gopDuration: { seconds: 2 },
                  profile: 'high',
                  preset: 'veryfast',
                },
              },
            },
            {
              key: 'video-480p',
              videoStream: {
                h264: {
                  heightPixels: 480,
                  widthPixels: 854,
                  bitrateBps: 1500000,
                  frameRate: 24,
                  gopDuration: { seconds: 2 },
                  profile: 'main',
                  preset: 'veryfast',
                },
              },
            },
            {
              key: 'video-360p',
              videoStream: {
                h264: {
                  heightPixels: 360,
                  widthPixels: 640,
                  bitrateBps: 800000,
                  frameRate: 24,
                  gopDuration: { seconds: 2 },
                  profile: 'main',
                  preset: 'veryfast',
                },
              },
            },
            // Audio stream
            {
              key: 'audio-aac',
              audioStream: {
                codec: 'aac',
                bitrateBps: 128000,
                sampleRateHertz: 48000,
                channelCount: 2,
              },
            },
          ],
          muxStreams: [
            // HLS mux for each quality
            { key: 'video-1080p-hls', container: 'ts', elementaryStreams: ['video-1080p', 'audio-aac'], segmentSettings: { segmentDuration: { seconds: 6 } } },
            { key: 'video-720p-hls', container: 'ts', elementaryStreams: ['video-720p', 'audio-aac'], segmentSettings: { segmentDuration: { seconds: 6 } } },
            { key: 'video-480p-hls', container: 'ts', elementaryStreams: ['video-480p', 'audio-aac'], segmentSettings: { segmentDuration: { seconds: 6 } } },
            { key: 'video-360p-hls', container: 'ts', elementaryStreams: ['video-360p', 'audio-aac'], segmentSettings: { segmentDuration: { seconds: 6 } } },
          ],
          manifests: [
            {
              fileName: 'master.m3u8',
              type: 'HLS' as const,
              muxStreams: ['video-1080p-hls', 'video-720p-hls', 'video-480p-hls', 'video-360p-hls'],
            },
          ],
        },
      },
    });

    const jobName = job.name!;
    this.logger.log(`[${movieId}] Transcoder job created: ${jobName}`);

    // Step 3: Poll for job completion
    await this.pollJobCompletion(movieId, jobName);

    // Step 4: Build HLS URL and update movie
    const hlsUrl = `https://storage.googleapis.com/${this.outputBucket}/${outputPrefix}master.m3u8`;

    await this.movieModel.findByIdAndUpdate(movieId, {
      hlsUrl,
      hlsStatus: 'completed',
    }, { runValidators: false });

    // Step 5: Clean up source file from GCS (save storage cost)
    await this.storage.bucket(this.sourceBucket).file(gcsSourcePath).delete().catch(() => {});

    this.logger.log(`[${movieId}] Transcoding completed: ${hlsUrl}`);
  }

  private async pollJobCompletion(movieId: string, jobName: string): Promise<void> {
    const maxWaitMs = 60 * 60 * 1000; // 1 hour max
    const pollIntervalMs = 15000; // 15 seconds
    const startTime = Date.now();

    while (Date.now() - startTime < maxWaitMs) {
      const [job] = await this.transcoderClient.getJob({ name: jobName });
      const state = job.state as string;

      this.logger.log(`[${movieId}] Job state: ${state}`);

      if (state === 'SUCCEEDED') return;
      if (state === 'FAILED') {
        const errorMsg = job.error?.message || 'Unknown transcoding error';
        throw new Error(`Transcoder job failed: ${errorMsg}`);
      }

      await new Promise((r) => setTimeout(r, pollIntervalMs));
    }

    throw new Error('Transcoder job timed out after 1 hour');
  }

  private uploadUrlToGcs(sourceUrl: string, bucket: string, destPath: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const file = this.storage.bucket(bucket).file(destPath);
      const writeStream = file.createWriteStream({
        resumable: true,
        contentType: 'video/mp4',
        metadata: { cacheControl: 'no-cache' },
      });

      writeStream.on('finish', () => resolve());
      writeStream.on('error', (err) => reject(err));

      this.downloadStream(sourceUrl, writeStream).catch(reject);
    });
  }

  private downloadStream(url: string, dest: NodeJS.WritableStream, maxRedirects = 10): Promise<void> {
    return new Promise((resolve, reject) => {
      if (maxRedirects <= 0) return reject(new Error('Too many redirects'));

      const client = url.startsWith('https') ? https : http;
      client.get(url, (response) => {
        const statusCode = response.statusCode ?? 0;

        if (statusCode >= 300 && statusCode < 400 && response.headers.location) {
          let redirectUrl = response.headers.location;
          if (redirectUrl.startsWith('/')) {
            const parsed = new URL(url);
            redirectUrl = `${parsed.protocol}//${parsed.host}${redirectUrl}`;
          }
          response.resume();
          return this.downloadStream(redirectUrl, dest, maxRedirects - 1).then(resolve).catch(reject);
        }

        if (statusCode !== 200) {
          response.resume();
          return reject(new Error(`Download failed: HTTP ${statusCode}`));
        }

        let downloaded = 0;
        response.on('data', (chunk) => {
          downloaded += chunk.length;
          if (downloaded % (100 * 1024 * 1024) < chunk.length) {
            this.logger.log(`  Uploaded ${Math.round(downloaded / 1024 / 1024)} MB to GCS...`);
          }
        });

        response.pipe(dest);
        response.on('end', () => resolve());
        response.on('error', reject);
      }).on('error', reject);
    });
  }

  private getFilenameFromUrl(url: string): string {
    try {
      const pathname = new URL(url).pathname;
      const filename = pathname.split('/').pop() || 'source.mp4';
      return decodeURIComponent(filename).replace(/[^a-zA-Z0-9._-]/g, '_');
    } catch {
      return 'source.mp4';
    }
  }
}
