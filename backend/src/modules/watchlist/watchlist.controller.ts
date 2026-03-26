import { Controller, Get, Post, Delete, Param, UseGuards, Headers } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { WatchlistService } from './watchlist.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@ApiTags('Watchlist')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'))
@Controller('watchlist')
export class WatchlistController {
  constructor(private readonly watchlistService: WatchlistService) {}

  @Get()
  @ApiOperation({ summary: 'Get user watchlist' })
  async getWatchlist(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
  ) {
    return this.watchlistService.getWatchlist(userId, profileId);
  }

  @Post(':contentId')
  @ApiOperation({ summary: 'Add to watchlist' })
  async add(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    await this.watchlistService.addToWatchlist(userId, profileId, contentId);
    return { message: 'Added to watchlist' };
  }

  @Delete(':contentId')
  @ApiOperation({ summary: 'Remove from watchlist' })
  async remove(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    await this.watchlistService.removeFromWatchlist(userId, profileId, contentId);
    return { message: 'Removed from watchlist' };
  }

  @Get(':contentId/check')
  @ApiOperation({ summary: 'Check if content is in watchlist' })
  async check(
    @CurrentUser('userId') userId: string,
    @Headers('x-profile-id') profileId: string,
    @Param('contentId') contentId: string,
  ) {
    const inWatchlist = await this.watchlistService.isInWatchlist(userId, profileId, contentId);
    return { inWatchlist };
  }
}
