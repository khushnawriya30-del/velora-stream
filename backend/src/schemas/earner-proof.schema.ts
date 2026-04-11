import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type EarnerProofDocument = EarnerProof & Document;

@Schema({ timestamps: true })
export class EarnerProof {
  @Prop({ required: true })
  imageUrl: string;

  @Prop({ default: '' })
  caption: string;

  @Prop({ default: 0 })
  displayOrder: number;

  @Prop({ default: true })
  isActive: boolean;
}

export const EarnerProofSchema = SchemaFactory.createForClass(EarnerProof);
