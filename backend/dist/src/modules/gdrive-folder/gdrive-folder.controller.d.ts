import { GdriveFolderService, ScanResult } from './gdrive-folder.service';
export declare class GdriveFolderController {
    private readonly gdriveFolderService;
    constructor(gdriveFolderService: GdriveFolderService);
    scanFolder(body: {
        folderUrl: string;
    }): Promise<ScanResult>;
    importToSeries(body: {
        seriesId: string;
        scanResult: ScanResult;
        folderUrl?: string;
    }): Promise<{
        seasonsCreated: number;
        episodesCreated: number;
    }>;
    refreshFromDrive(seriesId: string): Promise<{
        newEpisodes: number;
    }>;
}
