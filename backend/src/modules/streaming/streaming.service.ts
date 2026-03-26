import { Injectable, UnauthorizedException } from '@nestjs/common';
import { UploadService } from '../upload/upload.service';

@Injectable()
export class StreamingService {
  constructor(private uploadService: UploadService) {}

  getSignedStreamUrl(videoPath: string): { url: string } {
    const url = this.uploadService.generateSignedVideoUrl(videoPath);
    return { url };
  }

  verifyStreamAccess(videoPath: string, expires: number, signature: string): boolean {
    const valid = this.uploadService.verifySignedUrl(videoPath, expires, signature);
    if (!valid) {
      throw new UnauthorizedException('Invalid or expired stream URL');
    }
    return true;
  }
}
