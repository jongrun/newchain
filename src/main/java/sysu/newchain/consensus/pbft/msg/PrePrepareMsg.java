package sysu.newchain.consensus.pbft.msg;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.proto.BlockPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.core.Block;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.PrePreparePb;
import sysu.newchain.proto.PreparePb;
import sysu.newchain.proto.MsgWithSignPb.Builder;
import sysu.newchain.proto.MsgWithSignPb.MsgCase;

/**
 * @Description The message has the form (PRE-PREPARE, v, n, d, b)
 * where v indicates the view in which the message is being sent, 
 * b is Block of Transactions (the client’s request messages), 
 * n is the sequence number of b,
 * and d is b’s digest. 
 * @author jongliao
 * @date 2020年2月6日 下午6:43:38
 */
public class PrePrepareMsg extends MsgBuilder<PrePreparePb.Builder>{
	
	public PrePrepareMsg() {
		setBuilder(PrePreparePb.newBuilder());
	}
	
	public PrePrepareMsg(byte[] data) throws InvalidProtocolBufferException {
		setBuilder(PrePreparePb.parseFrom(data).toBuilder());
	}
	
	public long getView() {
		return getBuilder().getView();
	}
	
	public long getSeqNum() {
		return getBuilder().getSeqNum();
	}
	
	public byte[] getDigestOfBlock() {
		return getBuilder().getDigestOfBlock().toByteArray();
	}
	
	public BlockMsg getBlockMsg() {
		BlockMsg blockMsg = new BlockMsg();
		blockMsg.setBuilder(getBuilder().getBlockMsgBuilder());
		return blockMsg;
	}
	
	public Block getBlock() {
		return getBlockMsg().toBlock();
	}
	
	public void setView(long view) {
		getBuilder().setView(view);
	}
	
	public void setSeqNum(long seqNum) {
		getBuilder().setSeqNum(seqNum);
	}
	
	public void setDigestOfBlock(byte[] digest) {
		getBuilder().setDigestOfBlock(ByteString.copyFrom(digest));
	}
	
	public void setBlockMsg(BlockMsg blockMsg){
		getBuilder().setBlockMsg(blockMsg.getBuilder());
	}
	
	public void setBlock(Block block) {
		setBlockMsg(new BlockMsg(block));
	}
	
	public byte[] calculateDigestOfBlock() {
		return Hash.SHA256.hash(getBuilder().getBlockMsg().toByteArray());
	}
	
	public void calculateAndSetDigestOfBlock() {
		setDigestOfBlock(calculateDigestOfBlock());
	}
	
	public PrepareMsg createPrepareMsg(long replica) {
		PrepareMsg prepareMsg = new PrepareMsg();
		prepareMsg.setView(this.getView());
		prepareMsg.setSeqNum(this.getSeqNum());
		prepareMsg.setDigestOfBlock(this.getDigestOfBlock());
		prepareMsg.setReplica(replica);
		return prepareMsg;
	}
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}

	@Override
	public String toString() {
		return String.format("<Pre-prepare, view: %d, seqNum: %d, d: %s>", 
				getView(), getSeqNum(), Hex.encode(getDigestOfBlock()));
	}
	
//	public static void main(String[] args) {
//		MsgWithSignPb.Builder msgWithSignPb = MsgWithSignPb.newBuilder();
//		msgWithSignPb.getPrepareMsgBuilder().setView(1L);
//		System.out.println(msgWithSignPb.getPrepareMsg().getView());
//	}
}
