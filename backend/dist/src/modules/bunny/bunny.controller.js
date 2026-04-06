"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BunnyController = void 0;
const common_1 = require("@nestjs/common");
const platform_express_1 = require("@nestjs/platform-express");
const bunny_service_1 = require("./bunny.service");
const passport_1 = require("@nestjs/passport");
const roles_guard_1 = require("../auth/guards/roles.guard");
const roles_decorator_1 = require("../auth/decorators/roles.decorator");
let BunnyController = class BunnyController {
    constructor(bunnyService) {
        this.bunnyService = bunnyService;
    }
    async handleWebhook(body) {
        return this.bunnyService.handleWebhook(body);
    }
    async getLibraryStatus() {
        return this.bunnyService.getLibraryStatus();
    }
    async listVideos() {
        return this.bunnyService.listVideos();
    }
    async getVideoStatus(videoId) {
        return this.bunnyService.getVideoStatus(videoId);
    }
    async cleanupFailedVideos() {
        return this.bunnyService.cleanupFailedVideos();
    }
    getActiveJobs() {
        return this.bunnyService.getActiveJobs();
    }
    getProgress(jobId) {
        return this.bunnyService.getProgress(jobId);
    }
    async fetchMovieFromUrl(movieId, body) {
        return this.bunnyService.uploadMovieFromUrl(movieId, body.url);
    }
    async uploadMovieFile(movieId, file) {
        if (!file)
            throw new Error('No file provided');
        return this.bunnyService.uploadMovieFromFile(movieId, file.buffer, file.originalname);
    }
    async checkMovieTranscoding(movieId) {
        return this.bunnyService.checkMovieTranscoding(movieId);
    }
    async fetchEpisodeFromUrl(episodeId, body) {
        return this.bunnyService.uploadEpisodeFromUrl(episodeId, body.url);
    }
    async uploadEpisodeFile(episodeId, file) {
        if (!file)
            throw new Error('No file provided');
        return this.bunnyService.uploadEpisodeFromFile(episodeId, file.buffer, file.originalname);
    }
    async importSeasonFromFolder(seasonId, body) {
        return this.bunnyService.importSeasonFromFolder(seasonId, body.folderUrl);
    }
    async migrateSeasonToBunnyStream(seasonId) {
        return this.bunnyService.migrateSeasonToBunnyStream(seasonId);
    }
    async checkSeasonTranscoding(seasonId) {
        return this.bunnyService.checkSeasonTranscoding(seasonId);
    }
    async bulkUploadEpisodes(seasonId, body) {
        return this.bunnyService.bulkUploadEpisodes(seasonId, body.episodes);
    }
    async importFullSeries(seriesId, body) {
        return this.bunnyService.importFullSeries(seriesId, body.folderUrl);
    }
    async listCollections() {
        return this.bunnyService.listCollections();
    }
    async listVideosInCollection(collectionId) {
        return this.bunnyService.listVideosInCollection(collectionId);
    }
    async importFromBunnyCollection(seasonId, body) {
        return this.bunnyService.importFromBunnyCollection(seasonId, body.collectionId);
    }
    async importSeriesFromBunnyCollection(seriesId, body) {
        return this.bunnyService.importSeriesFromBunnyCollection(seriesId, body.collectionId);
    }
    async previewCollectionStructure(collectionId) {
        return this.bunnyService.previewBunnyCollectionStructure(collectionId);
    }
    async importMovieFromBunnyCollection(body) {
        try {
            if (!body.videoId) {
                throw new common_1.HttpException('videoId is required', common_1.HttpStatus.BAD_REQUEST);
            }
            return await this.bunnyService.importMovieFromBunnyVideo(body.videoId, body.title, body.existingMovieId);
        }
        catch (err) {
            common_1.Logger.error(`[Movie Import] ${err.message}`, err.stack, 'BunnyController');
            if (err instanceof common_1.HttpException)
                throw err;
            throw new common_1.HttpException(err.message || 'Failed to import movie from Bunny', common_1.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
};
exports.BunnyController = BunnyController;
__decorate([
    (0, common_1.Post)('stream/webhook'),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "handleWebhook", null);
__decorate([
    (0, common_1.Get)('stream/library'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "getLibraryStatus", null);
__decorate([
    (0, common_1.Get)('stream/videos'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "listVideos", null);
__decorate([
    (0, common_1.Get)('stream/video/:videoId'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('videoId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "getVideoStatus", null);
__decorate([
    (0, common_1.Post)('stream/cleanup'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "cleanupFailedVideos", null);
__decorate([
    (0, common_1.Get)('stream/jobs'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], BunnyController.prototype, "getActiveJobs", null);
__decorate([
    (0, common_1.Get)('stream/progress'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Query)('jobId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], BunnyController.prototype, "getProgress", null);
__decorate([
    (0, common_1.Post)('stream/movie/:movieId/fetch'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('movieId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "fetchMovieFromUrl", null);
__decorate([
    (0, common_1.Post)('stream/movie/:movieId/upload'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    (0, common_1.UseInterceptors)((0, platform_express_1.FileInterceptor)('file', { limits: { fileSize: 5 * 1024 * 1024 * 1024 } })),
    __param(0, (0, common_1.Param)('movieId')),
    __param(1, (0, common_1.UploadedFile)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "uploadMovieFile", null);
__decorate([
    (0, common_1.Get)('stream/movie/:movieId/status'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('movieId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "checkMovieTranscoding", null);
__decorate([
    (0, common_1.Post)('stream/episode/:episodeId/fetch'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('episodeId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "fetchEpisodeFromUrl", null);
__decorate([
    (0, common_1.Post)('stream/episode/:episodeId/upload'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    (0, common_1.UseInterceptors)((0, platform_express_1.FileInterceptor)('file', { limits: { fileSize: 5 * 1024 * 1024 * 1024 } })),
    __param(0, (0, common_1.Param)('episodeId')),
    __param(1, (0, common_1.UploadedFile)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "uploadEpisodeFile", null);
__decorate([
    (0, common_1.Post)('stream/season/:seasonId/import-folder'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seasonId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "importSeasonFromFolder", null);
__decorate([
    (0, common_1.Post)('stream/season/:seasonId/migrate'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seasonId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "migrateSeasonToBunnyStream", null);
__decorate([
    (0, common_1.Get)('stream/season/:seasonId/status'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seasonId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "checkSeasonTranscoding", null);
__decorate([
    (0, common_1.Post)('stream/season/:seasonId/bulk-upload'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seasonId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "bulkUploadEpisodes", null);
__decorate([
    (0, common_1.Post)('stream/series/:seriesId/import'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seriesId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "importFullSeries", null);
__decorate([
    (0, common_1.Get)('stream/collections'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "listCollections", null);
__decorate([
    (0, common_1.Get)('stream/collections/:collectionId/videos'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('collectionId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "listVideosInCollection", null);
__decorate([
    (0, common_1.Post)('stream/season/:seasonId/import-bunny'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seasonId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "importFromBunnyCollection", null);
__decorate([
    (0, common_1.Post)('stream/series/:seriesId/import-bunny-collection'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('seriesId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "importSeriesFromBunnyCollection", null);
__decorate([
    (0, common_1.Get)('stream/collections/:collectionId/preview'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Param)('collectionId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "previewCollectionStructure", null);
__decorate([
    (0, common_1.Post)('stream/movie/import-bunny'),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin'),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], BunnyController.prototype, "importMovieFromBunnyCollection", null);
exports.BunnyController = BunnyController = __decorate([
    (0, common_1.Controller)('bunny'),
    __metadata("design:paramtypes", [bunny_service_1.BunnyService])
], BunnyController);
//# sourceMappingURL=bunny.controller.js.map