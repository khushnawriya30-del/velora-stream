import { ConfigService } from '@nestjs/config';
import { Model } from 'mongoose';
import { AppVersion, AppVersionDocument } from './app-version.schema';
export declare class AppVersionService {
    private model;
    private configService;
    private readonly logger;
    constructor(model: Model<AppVersionDocument>, configService: ConfigService);
    private getGithubApkUrl;
    getLatest(): Promise<AppVersion>;
    resolveDownloadUrl(): Promise<string>;
    update(data: Partial<AppVersion>): Promise<AppVersion>;
}
