import {
  Controller,
  Post,
  Get,
  Param,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFile,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { BunnyService } from './bunny.service';
import { AuthGuard } from '@nestjs/passport';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('bunny')
@UseGuards(AuthGuard('jwt'), RolesGuard)
@Roles('admin')
export class BunnyController {
  constructor(private readonly bunnyService: BunnyService) {}

  // ─── Library Status ──────────────────────────────────────────

  @Get('stream/library')
  async getLibraryStatus() {
    return this.bunnyService.getLibraryStatus();
  }

  @Get('stream/videos')
  async listVideos() {
    return this.bunnyService.listVideos();
  }

  @Get('stream/video/:videoId')
  async getVideoStatus(@Param('videoId') videoId: string) {
    return this.bunnyService.getVideoStatus(videoId);
  }

  // ─── Progress Tracking ───────────────────────────────────────

  @Get('stream/progress')
  getProgress() {
    return this.bunnyService.getProgress();
  }

  // ─── Movie Upload ────────────────────────────────────────────

  /** Upload a movie to Bunny Stream from URL (fetches & transcodes) */
  @Post('stream/movie/:movieId/fetch')
  async fetchMovieFromUrl(
    @Param('movieId') movieId: string,
    @Body() body: { url?: string },
  ) {
    return this.bunnyService.uploadMovieFromUrl(movieId, body.url);
  }

  /** Upload a movie file directly to Bunny Stream */
  @Post('stream/movie/:movieId/upload')
  @UseInterceptors(FileInterceptor('file', { limits: { fileSize: 5 * 1024 * 1024 * 1024 } }))
  async uploadMovieFile(
    @Param('movieId') movieId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    if (!file) throw new Error('No file provided');
    return this.bunnyService.uploadMovieFromFile(movieId, file.buffer, file.originalname);
  }

  /** Check movie transcoding status */
  @Get('stream/movie/:movieId/status')
  async checkMovieTranscoding(@Param('movieId') movieId: string) {
    return this.bunnyService.checkMovieTranscoding(movieId);
  }

  // ─── Episode Upload ──────────────────────────────────────────

  /** Upload an episode to Bunny Stream from URL */
  @Post('stream/episode/:episodeId/fetch')
  async fetchEpisodeFromUrl(
    @Param('episodeId') episodeId: string,
    @Body() body: { url?: string },
  ) {
    return this.bunnyService.uploadEpisodeFromUrl(episodeId, body.url);
  }

  /** Upload an episode file directly */
  @Post('stream/episode/:episodeId/upload')
  @UseInterceptors(FileInterceptor('file', { limits: { fileSize: 5 * 1024 * 1024 * 1024 } }))
  async uploadEpisodeFile(
    @Param('episodeId') episodeId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    if (!file) throw new Error('No file provided');
    return this.bunnyService.uploadEpisodeFromFile(episodeId, file.buffer, file.originalname);
  }

  // ─── Season / Folder Import ──────────────────────────────────

  /** Import all episodes from a Google Drive folder to Bunny Stream */
  @Post('stream/season/:seasonId/import-folder')
  async importSeasonFromFolder(
    @Param('seasonId') seasonId: string,
    @Body() body: { folderUrl: string },
  ) {
    return this.bunnyService.importSeasonFromFolder(seasonId, body.folderUrl);
  }

  /** Migrate existing season episodes to Bunny Stream (re-fetch from current URLs) */
  @Post('stream/season/:seasonId/migrate')
  async migrateSeasonToBunnyStream(@Param('seasonId') seasonId: string) {
    return this.bunnyService.migrateSeasonToBunnyStream(seasonId);
  }

  /** Check transcoding status for all episodes in a season */
  @Get('stream/season/:seasonId/status')
  async checkSeasonTranscoding(@Param('seasonId') seasonId: string) {
    return this.bunnyService.checkSeasonTranscoding(seasonId);
  }

  // ─── Collections ─────────────────────────────────────────────

  @Get('stream/collections')
  async listCollections() {
    return this.bunnyService.listCollections();
  }
}
