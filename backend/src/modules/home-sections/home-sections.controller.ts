import { Controller, Get, Post, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { HomeSectionsService } from './home-sections.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';

@ApiTags('Home Sections')
@Controller('home')
export class HomeSectionsController {
  constructor(private readonly homeSectionsService: HomeSectionsService) {}

  @Get('feed')
  @ApiOperation({ summary: 'Get home feed with all sections and content' })
  async getFeed() {
    return this.homeSectionsService.getHomeFeed();
  }

  @Get('sections')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Get all sections (Admin)' })
  async getAll() {
    return this.homeSectionsService.getAll();
  }

  @Post('sections')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Create a section (Admin)' })
  async create(@Body() body: any) {
    return this.homeSectionsService.create(body);
  }

  @Patch('sections/:id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Update a section (Admin)' })
  async update(@Param('id') id: string, @Body() body: any) {
    return this.homeSectionsService.update(id, body);
  }

  @Delete('sections/:id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Delete a section (Admin)' })
  async delete(@Param('id') id: string) {
    await this.homeSectionsService.delete(id);
    return { message: 'Section deleted' };
  }

  @Post('sections/reorder')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'content_manager')
  @ApiOperation({ summary: 'Reorder sections (Admin)' })
  async reorder(@Body('orderedIds') orderedIds: string[]) {
    await this.homeSectionsService.reorder(orderedIds);
    return { message: 'Sections reordered' };
  }
}
