import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { EarnerProofsController } from './earner-proofs.controller';
import { EarnerProofsService } from './earner-proofs.service';
import { EarnerProof, EarnerProofSchema } from '../../schemas/earner-proof.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: EarnerProof.name, schema: EarnerProofSchema },
    ]),
  ],
  controllers: [EarnerProofsController],
  providers: [EarnerProofsService],
  exports: [EarnerProofsService],
})
export class EarnerProofsModule {}
