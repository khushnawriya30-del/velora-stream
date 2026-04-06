import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AppVersion, AppVersionDocument } from './app-version.schema';

@Injectable()
export class AppVersionService {
  constructor(
    @InjectModel(AppVersion.name) private model: Model<AppVersionDocument>,
  ) {}

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
    // Always return the GitHub Releases APK URL based on versionName
    const version = doc.versionName;
    doc.apkUrl = `https://github.com/khushnawriya30-del/velora-stream/releases/download/v${version}/Velora-v${version}.apk`;
    return doc;
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
