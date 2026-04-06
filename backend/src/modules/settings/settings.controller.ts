import {
  Controller,
  Get,
  Put,
  Post,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFile,
  Req,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { AuthGuard } from '@nestjs/passport';
import { SettingsService } from './settings.service';
import { AppSettings } from './settings.schema';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { diskStorage } from 'multer';
import { extname, join } from 'path';
import { existsSync, mkdirSync } from 'fs';

const uploadsDir = join(process.cwd(), 'public', 'uploads');
if (!existsSync(uploadsDir)) mkdirSync(uploadsDir, { recursive: true });

@Controller('settings')
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin')
export class SettingsController {
  constructor(private readonly service: SettingsService) {}

  @Get()
  getSettings(): Promise<AppSettings> {
    return this.service.getSettings();
  }

  @Put()
  updateSettings(@Body() body: Partial<AppSettings>): Promise<AppSettings> {
    return this.service.updateSettings(body);
  }

  @Post('upload-qr')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: uploadsDir,
        filename: (_req, file, cb) => {
          const ext = extname(file.originalname).toLowerCase();
          cb(null, `payment-qr${ext}`);
        },
      }),
      limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max
      fileFilter: (_req, file, cb) => {
        const allowed = /\.(jpg|jpeg|png|webp|gif)$/i;
        if (!allowed.test(extname(file.originalname))) {
          return cb(new Error('Only image files are allowed'), false);
        }
        cb(null, true);
      },
    }),
  )
  async uploadQrCode(@UploadedFile() file: Express.Multer.File, @Req() req: any) {
    const protocol = req.headers['x-forwarded-proto'] || req.protocol;
    const host = req.headers['x-forwarded-host'] || req.get('host');
    const publicUrl = `${protocol}://${host}/uploads/${file.filename}`;

    // Auto-save to settings
    await this.service.updateSettings({ paymentQrCodeUrl: publicUrl });

    return { url: publicUrl, filename: file.filename };
  }
}
