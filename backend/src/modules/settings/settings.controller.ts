import {
  Controller,
  Get,
  Put,
  Post,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { memoryStorage } from 'multer';
import { AuthGuard } from '@nestjs/passport';
import { SettingsService } from './settings.service';
import { AppSettings } from './settings.schema';
import { UpdateSettingsDto } from './dto/update-settings.dto';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { extname } from 'path';

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
  updateSettings(@Body() body: UpdateSettingsDto): Promise<AppSettings> {
    return this.service.updateSettings(body);
  }

  @Post('upload-qr')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: memoryStorage(),
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
  async uploadQrCode(@UploadedFile() file: Express.Multer.File) {
    // Convert to base64 data URL and store in MongoDB
    const mimeType = file.mimetype || 'image/png';
    const base64 = file.buffer.toString('base64');
    const dataUrl = `data:${mimeType};base64,${base64}`;

    await this.service.updateSettings({ paymentQrCodeUrl: dataUrl });

    return { url: dataUrl };
  }
}
