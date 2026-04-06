import {
  Controller,
  Post,
  Get,
  Body,
  Param,
  Query,
  UseGuards,
} from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { CurrentUser } from '../auth/decorators/current-user.decorator';
import { UpiPaymentService } from './upi-payment.service';
import { CreateOrderDto, SubmitUtrDto } from './dto/upi-payment.dto';

@Controller('upi-payment')
export class UpiPaymentController {
  constructor(private readonly upiPaymentService: UpiPaymentService) {}

  // ── Public / Auth endpoints ──

  @Post('create-order')
  @UseGuards(AuthGuard('jwt'))
  async createOrder(
    @Body() dto: CreateOrderDto,
    @CurrentUser('userId') userId: string,
  ) {
    return this.upiPaymentService.createOrder(dto.planId, userId, dto.deviceInfo);
  }

  @Post('submit-utr')
  @UseGuards(AuthGuard('jwt'))
  async submitUtr(
    @Body() dto: SubmitUtrDto,
    @CurrentUser('userId') userId: string,
  ) {
    return this.upiPaymentService.submitUtr(dto.orderId, dto.utrId, userId);
  }

  @Get('order/:orderId')
  @UseGuards(AuthGuard('jwt'))
  async getOrderStatus(
    @Param('orderId') orderId: string,
    @CurrentUser('userId') userId: string,
  ) {
    return this.upiPaymentService.getOrderStatus(orderId, userId);
  }

  @Get('my-orders')
  @UseGuards(AuthGuard('jwt'))
  async getMyOrders(@CurrentUser('userId') userId: string) {
    return this.upiPaymentService.getUserOrders(userId);
  }

  // ── Admin endpoints ──

  @Get('admin/payments')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getPayments(
    @Query('status') status?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
  ) {
    return this.upiPaymentService.getPayments({
      status,
      page: page ? parseInt(page, 10) : 1,
      limit: limit ? parseInt(limit, 10) : 50,
    });
  }

  @Post('admin/verify/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async verifyPayment(@Param('id') id: string) {
    return this.upiPaymentService.adminVerify(id);
  }

  @Post('admin/reject/:id')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async rejectPayment(
    @Param('id') id: string,
    @Body('reason') reason?: string,
  ) {
    return this.upiPaymentService.adminReject(id, reason);
  }

  @Get('admin/stats')
  @UseGuards(AuthGuard('jwt'), RolesGuard)
  @Roles('admin')
  async getStats() {
    return this.upiPaymentService.getStats();
  }
}
