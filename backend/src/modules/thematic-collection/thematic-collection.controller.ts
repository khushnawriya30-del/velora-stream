import { Controller, Get, Post, Delete, Param, UseGuards, Headers } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ThematicCollectionService } from './thematic-collection.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@ApiTags('Thematic Collection')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'))
@Controller('thematic-collection')
export class ThematicCollectionController {
  constructor(private readonly collectionService: ThematicCollectionService) {}

  @Get()
  @ApiOperation({ summary: 'Get user thematic collection' })
  async getCollection(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
  ) {
    return this.collectionService.getCollection(userId, profileId);
  }

  @Post(':contentId')
  @ApiOperation({ summary: 'Add to thematic collection' })
  async add(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    await this.collectionService.addToCollection(userId, profileId, contentId);
    return { message: 'Added to thematic collection' };
  }

  @Delete(':contentId')
  @ApiOperation({ summary: 'Remove from thematic collection' })
  async remove(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    await this.collectionService.removeFromCollection(userId, profileId, contentId);
    return { message: 'Removed from thematic collection' };
  }

  @Get(':contentId/check')
  @ApiOperation({ summary: 'Check if content is in thematic collection' })
  async check(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    const inCollection = await this.collectionService.isInCollection(userId, profileId, contentId);
    return { inCollection };
  }
}
