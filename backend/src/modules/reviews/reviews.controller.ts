import { Controller, Get, Post, Patch, Delete, Body, Param, Query, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ReviewsService } from './reviews.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { Roles } from '../auth/decorators/roles.decorator';
import { RolesGuard } from '../auth/guards/roles.guard';
import { ModerationStatus } from '../../schemas/review.schema';

@ApiTags('Reviews')
@Controller('reviews')
export class ReviewsController {
  constructor(private readonly reviewsService: ReviewsService) {}

  @Get('content/:contentId')
  @ApiOperation({ summary: 'Get approved reviews for content' })
  async getReviews(
    @Param('contentId') contentId: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.reviewsService.getReviews(contentId, page, limit);
  }

  @Post()
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'))
  @ApiOperation({ summary: 'Submit a review' })
  async createReview(
    @CurrentUser('userId') userId: string,
    @Body() body: { contentId: string; rating: number; text?: string },
  ) {
    return this.reviewsService.createReview(userId, body.contentId, body.rating, body.text);
  }

  // Admin moderation
  @Get('admin/all')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'moderator')
  @ApiOperation({ summary: 'Get all reviews for moderation (Admin)' })
  async getAllReviews(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('status') status?: ModerationStatus,
  ) {
    return this.reviewsService.getAllReviews(page, limit, status);
  }

  @Patch(':id/moderate')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'moderator')
  @ApiOperation({ summary: 'Moderate a review (Admin)' })
  async moderate(
    @Param('id') id: string,
    @CurrentUser('userId') moderatorId: string,
    @Body('status') status: ModerationStatus,
  ) {
    return this.reviewsService.moderateReview(id, status, moderatorId);
  }

  @Delete(':id')
  @ApiBearerAuth()
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin', 'moderator')
  @ApiOperation({ summary: 'Delete a review (Admin)' })
  async delete(@Param('id') id: string) {
    await this.reviewsService.deleteReview(id);
    return { message: 'Review deleted' };
  }
}
