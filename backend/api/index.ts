import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import * as cookieParser from 'cookie-parser';
import helmet from 'helmet';
import { AppModule } from '../src/app.module';
import { NestExpressApplication } from '@nestjs/platform-express';

let app: NestExpressApplication;

async function getApp() {
  if (!app) {
    app = await NestFactory.create<NestExpressApplication>(AppModule, {
      logger: ['error', 'warn'],
    });

    app.use(helmet());
    app.use(cookieParser());

    app.enableCors({
      origin: true,
      credentials: true,
      methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
      allowedHeaders: ['Content-Type', 'Authorization', 'x-profile-id'],
    });

    app.setGlobalPrefix('api/v1');

    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
        transformOptions: { enableImplicitConversion: true },
      }),
    );

    const swaggerConfig = new DocumentBuilder()
      .setTitle('CineVault API')
      .setDescription('CineVault Premium Streaming Platform API')
      .setVersion('1.0')
      .addBearerAuth()
      .build();
    const document = SwaggerModule.createDocument(app, swaggerConfig);
    SwaggerModule.setup('docs', app, document);

    await app.init();
  }
  return app;
}

export default async function handler(req: any, res: any) {
  const nestApp = await getApp();
  const instance = nestApp.getHttpAdapter().getInstance();
  instance(req, res);
}
