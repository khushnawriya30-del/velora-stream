"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.GdriveFolderModule = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const gdrive_folder_controller_1 = require("./gdrive-folder.controller");
const gdrive_folder_service_1 = require("./gdrive-folder.service");
const series_module_1 = require("../series/series.module");
const movie_schema_1 = require("../../schemas/movie.schema");
let GdriveFolderModule = class GdriveFolderModule {
};
exports.GdriveFolderModule = GdriveFolderModule;
exports.GdriveFolderModule = GdriveFolderModule = __decorate([
    (0, common_1.Module)({
        imports: [
            series_module_1.SeriesModule,
            mongoose_1.MongooseModule.forFeature([{ name: movie_schema_1.Movie.name, schema: movie_schema_1.MovieSchema }]),
        ],
        controllers: [gdrive_folder_controller_1.GdriveFolderController],
        providers: [gdrive_folder_service_1.GdriveFolderService],
    })
], GdriveFolderModule);
//# sourceMappingURL=gdrive-folder.module.js.map