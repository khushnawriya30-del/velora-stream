import { Controller, Get, Post, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { EarnerProofsService } from './earner-proofs.service';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';

@ApiTags('Earner Proofs')
@Controller('earner-proofs')
export class EarnerProofsController {
  constructor(private readonly service: EarnerProofsService) {}

  @Get()
  @ApiOperation({ summary: 'Get active earner proof screenshots (public)' })
  async getActive() {
    return this.service.getActive();
  }

  @Get('all')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @ApiOperation({ summary: 'Get all earner proofs (Admin)' })
  async getAll() {
    return this.service.getAll();
  }

  @Post()
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @ApiOperation({ summary: 'Create earner proof (Admin)' })
  async create(@Body() body: { imageUrl: string; caption?: string }) {
    return this.service.create(body);
  }

  @Patch(':id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @ApiOperation({ summary: 'Update earner proof (Admin)' })
  async update(@Param('id') id: string, @Body() body: Partial<{ imageUrl: string; caption: string; isActive: boolean }>) {
    return this.service.update(id, body);
  }

  @Delete(':id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @ApiOperation({ summary: 'Delete earner proof (Admin)' })
  async delete(@Param('id') id: string) {
    await this.service.delete(id);
    return { message: 'Proof deleted' };
  }

  @Post('reorder')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  @ApiOperation({ summary: 'Reorder earner proofs (Admin)' })
  async reorder(@Body('orderedIds') orderedIds: string[]) {
    await this.service.reorder(orderedIds);
    return { message: 'Proofs reordered' };
  }
}
