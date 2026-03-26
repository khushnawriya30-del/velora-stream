import { Controller, Get, Post, Patch, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ProfilesService } from './profiles.service';
import { CurrentUser } from '../auth/decorators/current-user.decorator';

@ApiTags('Profiles')
@ApiBearerAuth()
@UseGuards(AuthGuard('jwt'))
@Controller('profiles')
export class ProfilesController {
  constructor(private readonly profilesService: ProfilesService) {}

  @Get()
  @ApiOperation({ summary: 'Get all profiles for current user' })
  async getProfiles(@CurrentUser('userId') userId: string) {
    return this.profilesService.getProfiles(userId);
  }

  @Post()
  @ApiOperation({ summary: 'Create a new profile' })
  async createProfile(
    @CurrentUser('userId') userId: string,
    @Body() body: { displayName: string; avatarUrl?: string; maturityRating?: string; pin?: string },
  ) {
    return this.profilesService.createProfile(userId, body);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update a profile' })
  async updateProfile(
    @CurrentUser('userId') userId: string,
    @Param('id') profileId: string,
    @Body() body: { displayName?: string; avatarUrl?: string; maturityRating?: string; pin?: string },
  ) {
    return this.profilesService.updateProfile(userId, profileId, body);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a profile' })
  async deleteProfile(
    @CurrentUser('userId') userId: string,
    @Param('id') profileId: string,
  ) {
    await this.profilesService.deleteProfile(userId, profileId);
    return { message: 'Profile deleted' };
  }

  @Post(':id/verify-pin')
  @ApiOperation({ summary: 'Verify profile PIN' })
  async verifyPin(@Param('id') profileId: string, @Body('pin') pin: string) {
    const valid = await this.profilesService.verifyPin(profileId, pin);
    return { valid };
  }
}
