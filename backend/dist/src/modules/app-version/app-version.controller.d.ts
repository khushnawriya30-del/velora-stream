import { Response } from 'express';
import { AppVersionService } from './app-version.service';
import { AppVersion } from './app-version.schema';
export declare class AppVersionController {
    private readonly service;
    constructor(service: AppVersionService);
    getLatest(): Promise<AppVersion>;
    download(res: Response): Promise<void>;
    update(body: Partial<AppVersion>): Promise<AppVersion>;
}
