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
exports.GdriveFolderController = void 0;
const common_1 = require("@nestjs/common");
const passport_1 = require("@nestjs/passport");
const swagger_1 = require("@nestjs/swagger");
const gdrive_folder_service_1 = require("./gdrive-folder.service");
const roles_decorator_1 = require("../auth/decorators/roles.decorator");
const roles_guard_1 = require("../auth/guards/roles.guard");
let GdriveFolderController = class GdriveFolderController {
    constructor(gdriveFolderService) {
        this.gdriveFolderService = gdriveFolderService;
    }
    async scanFolder(body) {
        return this.gdriveFolderService.scanFolder(body.folderUrl);
    }
    async importToSeries(body) {
        return this.gdriveFolderService.importToSeries(body.seriesId, body.scanResult, body.folderUrl);
    }
    async refreshFromDrive(seriesId) {
        return this.gdriveFolderService.refreshFromDrive(seriesId);
    }
};
exports.GdriveFolderController = GdriveFolderController;
__decorate([
    (0, common_1.Post)('scan'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin', 'content_manager'),
    (0, swagger_1.ApiOperation)({ summary: 'Scan a public Google Drive folder and detect episodes (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], GdriveFolderController.prototype, "scanFolder", null);
__decorate([
    (0, common_1.Post)('import'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin', 'content_manager'),
    (0, swagger_1.ApiOperation)({ summary: 'Import scanned episodes into a series (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], GdriveFolderController.prototype, "importToSeries", null);
__decorate([
    (0, common_1.Post)('refresh/:seriesId'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin', 'content_manager'),
    (0, swagger_1.ApiOperation)({ summary: 'Re-scan Drive folder and add new episodes (Admin)' }),
    __param(0, (0, common_1.Param)('seriesId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], GdriveFolderController.prototype, "refreshFromDrive", null);
exports.GdriveFolderController = GdriveFolderController = __decorate([
    (0, swagger_1.ApiTags)('Google Drive Folder'),
    (0, common_1.Controller)('gdrive-folder'),
    __metadata("design:paramtypes", [gdrive_folder_service_1.GdriveFolderService])
], GdriveFolderController);
//# sourceMappingURL=gdrive-folder.controller.js.map