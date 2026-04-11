import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EarnerProof, EarnerProofDocument } from '../../schemas/earner-proof.schema';

@Injectable()
export class EarnerProofsService {
  constructor(
    @InjectModel(EarnerProof.name) private proofModel: Model<EarnerProofDocument>,
  ) {}

  async getActive(): Promise<EarnerProofDocument[]> {
    return this.proofModel.find({ isActive: true }).sort({ displayOrder: 1 });
  }

  async getAll(): Promise<EarnerProofDocument[]> {
    return this.proofModel.find().sort({ displayOrder: 1 });
  }

  async create(data: Partial<EarnerProof>): Promise<EarnerProofDocument> {
    return this.proofModel.create(data);
  }

  async update(id: string, data: Partial<EarnerProof>): Promise<EarnerProofDocument> {
    const proof = await this.proofModel.findByIdAndUpdate(id, data, { new: true });
    if (!proof) throw new NotFoundException('Proof not found');
    return proof;
  }

  async delete(id: string): Promise<void> {
    const result = await this.proofModel.findByIdAndDelete(id);
    if (!result) throw new NotFoundException('Proof not found');
  }

  async reorder(orderedIds: string[]): Promise<void> {
    const bulkOps = orderedIds.map((id, index) => ({
      updateOne: {
        filter: { _id: id },
        update: { displayOrder: index },
      },
    }));
    await this.proofModel.bulkWrite(bulkOps);
  }
}
