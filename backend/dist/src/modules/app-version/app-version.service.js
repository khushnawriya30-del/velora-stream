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
var AppVersionService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppVersionService = void 0;
const common_1 = require("@nestjs/common");
const mongoose_1 = require("@nestjs/mongoose");
const config_1 = require("@nestjs/config");
const mongoose_2 = require("mongoose");
const app_version_schema_1 = require("./app-version.schema");
let AppVersionService = AppVersionService_1 = class AppVersionService {
    constructor(model, configService) {
        this.model = model;
        this.configService = configService;
        this.logger = new common_1.Logger(AppVersionService_1.name);
    }
    getGithubApkUrl(version) {
        return `https://github.com/khushnawriya30-del/velora-stream/releases/download/latest/CineVault-v${version}.apk`;
    }
    async getLatest() {
        let doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
        if (!doc) {
            doc = await this.model.create({
                versionCode: 1,
                versionName: '1.0.0',
                forceUpdate: false,
                apkUrl: '',
                releaseNotes: 'Initial release',
            });
        }
        const apiPrefix = this.configService.get('API_PREFIX', 'api/v1');
        const backendUrl = this.configService.get('BACKEND_URL', 'https://p2zb77xpuy.ap-south-1.awsapprunner.com');
        doc.apkUrl = `${backendUrl}/${apiPrefix}/app-version/download`;
        return doc;
    }
    async resolveDownloadUrl() {
        const doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
        const version = doc?.versionName || '2.0.3';
        const githubUrl = this.getGithubApkUrl(version);
        try {
            const resp = await fetch(githubUrl, { method: 'HEAD', redirect: 'follow' });
            if (resp.ok && resp.url) {
                this.logger.log(`Resolved APK URL: ${resp.url.substring(0, 80)}...`);
                return resp.url;
            }
        }
        catch (err) {
            this.logger.warn(`Failed to resolve redirect for ${githubUrl}: ${err.message}`);
        }
        return githubUrl;
    }
    async update(data) {
        const doc = await this.model.findOne().sort({ versionCode: -1 }).exec();
        if (doc) {
            Object.assign(doc, data);
            return doc.save();
        }
        return this.model.create(data);
    }
};
exports.AppVersionService = AppVersionService;
exports.AppVersionService = AppVersionService = AppVersionService_1 = __decorate([
    (0, common_1.Injectable)(),
    __param(0, (0, mongoose_1.InjectModel)(app_version_schema_1.AppVersion.name)),
    __metadata("design:paramtypes", [mongoose_2.Model,
        config_1.ConfigService])
], AppVersionService);
//# sourceMappingURL=app-version.service.js.map