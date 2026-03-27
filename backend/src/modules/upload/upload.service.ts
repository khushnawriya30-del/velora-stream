import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { S3Client, PutObjectCommand, GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { v4 as uuidv4 } from 'uuid';
import * as crypto from 'crypto';
import { createReadStream } from 'fs';

@Injectable()
export class UploadService {
  private s3Client: S3Client;
  private bucket: string;
  private cdnBaseUrl: string;

  constructor(private configService: ConfigService) {
    this.s3Client = new S3Client({
      region: configService.get<string>('S3_REGION', 'us-east-1'),
      endpoint: configService.get<string>('S3_ENDPOINT'),
      credentials: {
        accessKeyId: configService.get<string>('S3_ACCESS_KEY', ''),
        secretAccessKey: configService.get<string>('S3_SECRET_KEY', ''),
      },
    });
    this.bucket = configService.get<string>('S3_BUCKET', 'cinevault-media');
    this.cdnBaseUrl = configService.get<string>('CDN_BASE_URL', '');
  }

  async getPresignedUploadUrl(
    folder: string,
    filename: string,
    contentType: string,
  ): Promise<{ uploadUrl: string; key: string; publicUrl: string }> {
    const ext = filename.split('.').pop();
    const key = `${folder}/${uuidv4()}.${ext}`;

    const command = new PutObjectCommand({
      Bucket: this.bucket,
      Key: key,
      ContentType: contentType,
    });

    const uploadUrl = await getSignedUrl(this.s3Client, command, { expiresIn: 3600 });
    const publicUrl = this.cdnBaseUrl ? `${this.cdnBaseUrl}/${key}` : `https://${this.bucket}.s3.amazonaws.com/${key}`;

    return { uploadUrl, key, publicUrl };
  }

  async uploadFileFromDisk(key: string, filePath: string, contentType: string): Promise<string> {
    const fileStream = createReadStream(filePath);
    const command = new PutObjectCommand({
      Bucket: this.bucket,
      Key: key,
      Body: fileStream,
      ContentType: contentType,
    });
    await this.s3Client.send(command);
    return this.cdnBaseUrl ? `${this.cdnBaseUrl}/${key}` : `https://${this.bucket}.s3.amazonaws.com/${key}`;
  }

  generateSignedVideoUrl(videoPath: string): string {
    const secret = this.configService.get<string>('VIDEO_SIGN_SECRET', 'default-secret');
    const expiry = Math.floor(Date.now() / 1000) + this.configService.get<number>('VIDEO_URL_EXPIRY', 3600);

    const dataToSign = `${videoPath}:${expiry}`;
    const signature = crypto.createHmac('sha256', secret).update(dataToSign).digest('hex');

    const baseUrl = this.cdnBaseUrl || `https://${this.bucket}.s3.amazonaws.com`;
    return `${baseUrl}/${videoPath}?expires=${expiry}&signature=${signature}`;
  }

  verifySignedUrl(videoPath: string, expires: number, signature: string): boolean {
    if (expires < Math.floor(Date.now() / 1000)) return false;

    const secret = this.configService.get<string>('VIDEO_SIGN_SECRET', 'default-secret');
    const dataToSign = `${videoPath}:${expires}`;
    const expectedSignature = crypto.createHmac('sha256', secret).update(dataToSign).digest('hex');

    return crypto.timingSafeEqual(
      Buffer.from(signature, 'hex'),
      Buffer.from(expectedSignature, 'hex'),
    );
  }
}
