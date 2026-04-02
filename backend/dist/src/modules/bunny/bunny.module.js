"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BunnyModule = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const platform_express_1 = require("@nestjs/platform-express");
const movie_schema_1 = require("../../schemas/movie.schema");
const series_schema_1 = require("../../schemas/series.schema");
const bunny_service_1 = require("./bunny.service");
const bunny_controller_1 = require("./bunny.controller");
let BunnyModule = class BunnyModule {
};
exports.BunnyModule = BunnyModule;
exports.BunnyModule = BunnyModule = __decorate([
    (0, common_1.Module)({
        imports: [
            mongoose_1.MongooseModule.forFeature([
                { name: movie_schema_1.Movie.name, schema: movie_schema_1.MovieSchema },
                { name: series_schema_1.Season.name, schema: series_schema_1.SeasonSchema },
                { name: series_schema_1.Episode.name, schema: series_schema_1.EpisodeSchema },
            ]),
            platform_express_1.MulterModule.register({ limits: { fileSize: 5 * 1024 * 1024 * 1024 } }),
        ],
        controllers: [bunny_controller_1.BunnyController],
        providers: [bunny_service_1.BunnyService],
        exports: [bunny_service_1.BunnyService],
    })
], BunnyModule);
//# sourceMappingURL=bunny.module.js.map