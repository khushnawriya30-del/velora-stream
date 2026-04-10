import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { AppVersion, AppVersionDocument } from './app-version.schema';

@Injectable()
export class AppVersionService {
  private readonly logger = new Logger(AppVersionService.name);

  constructor(
    @InjectModel(AppVersion.name) private model: Model<AppVersionDocument>,
    private configService: ConfigService,
  ) {}

  private getGithubApkUrl(version: string): string {
    return `https://github.com/khushnawriya30-del/velora-stream/releases/download/v${version}/app-release.apk`;
  }

  async getLatest(): Promise<AppVersion> {
    let doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
    if (!doc) {
      doc = await this.model.create({
        versionCode: 1,
        versionName: '1.0.0',
        forceUpdate: false,
        apkUrl: '',
        releaseNotes: 'Initial release',
      });
    }
    // Point apkUrl to our backend download endpoint (resolves GitHub redirect server-side)
    // This avoids DownloadManager issues with GitHub's 302 → long SAS CDN URLs
    const apiPrefix = this.configService.get<string>('API_PREFIX', 'api/v1');
    const backendUrl = this.configService.get<string>(
      'BACKEND_URL',
      'https://p2zb77xpuy.ap-south-1.awsapprunner.com',
    );
    doc.apkUrl = `${backendUrl}/${apiPrefix}/app-version/download`;
    return doc;
  }

  /**
   * Follow GitHub Releases redirect to get the direct CDN download URL.
   * GitHub returns 302 → release-assets.githubusercontent.com with SAS token.
   */
  async resolveDownloadUrl(): Promise<string> {
    const doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
    const version = doc?.versionName || '2.0.3';
    const githubUrl = this.getGithubApkUrl(version);

    try {
      const resp = await fetch(githubUrl, { method: 'HEAD', redirect: 'follow' });
      // After following redirects, resp.url is the final direct CDN URL
      if (resp.ok && resp.url) {
        this.logger.log(`Resolved APK URL: ${resp.url.substring(0, 80)}...`);
        return resp.url;
      }
    } catch (err) {
      this.logger.warn(`Failed to resolve redirect for ${githubUrl}: ${err.message}`);
    }
    // Fallback to the raw GitHub URL
    return githubUrl;
  }

  async update(data: Partial<AppVersion>): Promise<AppVersion> {
    const doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
    if (doc) {
      Object.assign(doc, data);
      return doc.save();
    }
    return this.model.create(data);
  }
}
