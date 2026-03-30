import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AppVersion, AppVersionSchema } from './app-version.schema';
import { AppVersionService } from './app-version.service';
import { AppVersionController } from './app-version.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: AppVersion.name, schema: AppVersionSchema }]),
  ],
  controllers: [AppVersionController],
  providers: [AppVersionService],
  exports: [AppVersionService],
})
export class AppVersionModule {}
