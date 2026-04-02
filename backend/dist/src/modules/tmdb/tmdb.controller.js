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
exports.TmdbController = void 0;
const common_1 = require("@nestjs/common");
const passport_1 = require("@nestjs/passport");
const swagger_1 = require("@nestjs/swagger");
const tmdb_service_1 = require("./tmdb.service");
const roles_decorator_1 = require("../auth/decorators/roles.decorator");
const roles_guard_1 = require("../auth/guards/roles.guard");
let TmdbController = class TmdbController {
    constructor(tmdbService) {
        this.tmdbService = tmdbService;
    }
    async discover(body) {
        const count = body.count || 20;
        return this.tmdbService.discover({ ...body, count });
    }
    async searchPerson(body) {
        return this.tmdbService.searchPerson(body.query);
    }
    async search(body) {
        return this.tmdbService.search(body);
    }
    async importItems(body) {
        if (!body.tmdbIds?.length)
            return { imported: 0, skipped: 0, items: [] };
        return this.tmdbService.importItems(body.tmdbIds, body.contentType, body.asUpcoming ?? false);
    }
};
exports.TmdbController = TmdbController;
__decorate([
    (0, common_1.Post)('discover'),
    (0, swagger_1.ApiOperation)({ summary: 'Discover/preview content from TMDB with filters (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], TmdbController.prototype, "discover", null);
__decorate([
    (0, common_1.Post)('search-person'),
    (0, swagger_1.ApiOperation)({ summary: 'Search TMDB for actors/directors by name (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], TmdbController.prototype, "searchPerson", null);
__decorate([
    (0, common_1.Post)('search'),
    (0, swagger_1.ApiOperation)({ summary: 'Search TMDB by title/name (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], TmdbController.prototype, "search", null);
__decorate([
    (0, common_1.Post)('import'),
    (0, swagger_1.ApiOperation)({ summary: 'Import selected TMDB items into database (Admin)' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], TmdbController.prototype, "importItems", null);
exports.TmdbController = TmdbController = __decorate([
    (0, swagger_1.ApiTags)('TMDB'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)((0, passport_1.AuthGuard)('jwt'), roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('admin', 'content_manager'),
    (0, common_1.Controller)('tmdb'),
    __metadata("design:paramtypes", [tmdb_service_1.TmdbService])
], TmdbController);
//# sourceMappingURL=tmdb.controller.js.map