import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type AdminLogDocument = AdminLog & Document;

@Schema({ timestamps: true })
export class AdminLog {
  @Prop({ required: true })
  adminId: string;

  @Prop({ required: true })
  adminEmail: string;

  @Prop({ required: true })
  action: string;

  @Prop()
  resource: string;

  @Prop()
  resourceId: string;

  @Prop({ type: Object })
  details: Record<string, any>;

  @Prop()
  ipAddress: string;
}

export const AdminLogSchema = SchemaFactory.createForClass(AdminLog);
AdminLogSchema.index({ adminId: 1, createdAt: -1 });
AdminLogSchema.index({ action: 1 });
