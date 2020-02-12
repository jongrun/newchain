package sysu.newchain.consensus.pbft.msg;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.proto.BlockPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.core.Block;
import sysu.newchain.core.Transaction;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;

public class BlockMsg extends MsgBuilder<BlockPb.Builder>{
	public static final Logger logger = LoggerFactory.getLogger(BlockMsg.class);
	
	public BlockMsg() {
		setBuilder(BlockPb.newBuilder());
	}
	
	public BlockMsg(byte[] data) throws InvalidProtocolBufferException {
		setBuilder(BlockPb.parseFrom(data).toBuilder());
	}
	
	public BlockMsg(Block block) {
		BlockPbCloner blockPbCloner = (BlockPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.BLOCK);
		BlockPb blockPb = blockPbCloner.toProto(block);
		setBuilder(blockPb.toBuilder());
	}
	
	public Block toBlock() {
		BlockPbCloner blockPbCloner = (BlockPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.BLOCK);
		Block block = blockPbCloner.toObject(getBuilder().build());
		return block;
	}
	
	public List<MsgWithSign> getTxMsgWithSignList() {
		List<MsgWithSignPb> msgWithSignPbs = getBuilder().getTxMsgWithSignList();
		List<MsgWithSign> msgWithSigns= new ArrayList<>(msgWithSignPbs.size());
		for (MsgWithSignPb msgWithSignPb : msgWithSignPbs) {
			MsgWithSign msgWithSign = new MsgWithSign();
			msgWithSign.setBuilder(msgWithSignPb.toBuilder());
			msgWithSigns.add(msgWithSign);
		}
		return msgWithSigns;
	}
	
	public boolean verifyTxSigns() {
		List<MsgWithSign> txMsgWithSigns = getTxMsgWithSignList();
		for (MsgWithSign txMsgWithSign: txMsgWithSigns) {
			TxMsg txMsg = txMsgWithSign.getTxMsg();
			if (!txMsgWithSign.verifySign(txMsg.getBuilder().getClientPubKey().toByteArray())) {
				logger.error("verify sign of tx msg from client failed, txhash: {}, client pubKey: {}", txMsg.getBuilder().getHash(), txMsg.getBuilder().getClientPubKey());
				return false;
			}
		}
		return true;
	}
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}

}
