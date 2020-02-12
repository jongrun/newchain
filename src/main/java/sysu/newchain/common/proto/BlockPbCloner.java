package sysu.newchain.common.proto;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;
import sysu.newchain.proto.BlockHeaderPb;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.TransactionPb;

public class BlockPbCloner implements ProtoCloner<Block, BlockPb>{

	@Override
	public Block toObject(BlockPb p) {
		Block block = new Block();
		BlockHeader header = new BlockHeader();
		header.setHeight(p.getHeader().getHeight());
		header.setHash(p.getHeader().getHash().toByteArray());
		header.setPrehash(p.getHeader().getPreHash().toByteArray());
		header.setTime(p.getHeader().getTime());
		block.setHeader(header);
		List<MsgWithSignPb> transactions = p.getTxMsgWithSignList();
		if (transactions != null) {
			List<Transaction> txList = new ArrayList<Transaction>(transactions.size());
			for (int i = 0; i < transactions.size(); i++) {
				TransactionPbCloner cloner = (TransactionPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.TRANSACTION);
				txList.add(cloner.toObject(transactions.get(i)));
			}
			block.setTransactions(txList);
		}
		else {
			block.setTransactions(new ArrayList<Transaction>());
		}
		return block;
	}

	@Override
	public BlockPb toProto(Block o) {
		BlockPb.Builder blockBuilder = BlockPb.newBuilder();
		BlockHeaderPb.Builder headerBuilder = BlockHeaderPb.newBuilder();
		BlockHeader header = o.getHeader();
		headerBuilder.setHeight(header.getHeight());
		headerBuilder.setHash(ByteString.copyFrom(header.getHash()));
		headerBuilder.setPreHash(ByteString.copyFrom(header.getPrehash()));
		headerBuilder.setTime(header.getTime());
		blockBuilder.setHeader(headerBuilder);
		List<Transaction> transactions = o.getTransactions();
		if (transactions != null) {
			for (Transaction transaction : transactions) {
				TransactionPbCloner cloner = (TransactionPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.TRANSACTION);
				MsgWithSignPb txPb = cloner.toProto(transaction);
				blockBuilder.addTxMsgWithSign(txPb);
			}
		}
		return blockBuilder.build();
	}

}
