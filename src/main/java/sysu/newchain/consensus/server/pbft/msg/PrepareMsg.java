package sysu.newchain.consensus.server.pbft.msg;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Hex;
import sysu.newchain.proto.MsgWithSignPb.MsgCase;
import sysu.newchain.proto.PreparePb;

/**
 * @Description The message has the form (PREPARE, v, n, d, i)
 * where v indicates the view in which the message is being sent,  
 * n is the sequence number of b,
 * d is block’s digest, 
 * and i is the replica who sent the message.
 * @author jongliao
 * @date 2020年2月6日 下午6:43:50
 */
public class PrepareMsg extends MsgBuilder<PreparePb.Builder>{
	
	public PrepareMsg() {
		setBuilder(PreparePb.newBuilder());
	}
	
	public PrepareMsg(byte[] data) throws InvalidProtocolBufferException{
		setBuilder(PreparePb.parseFrom(data).toBuilder());
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
	
	public void setView(long view) {
		getBuilder().setView(view);
	}
	
	public void setSeqNum(long seqNum) {
		getBuilder().setSeqNum(seqNum);
	}
	
	public void setDigestOfBlock(byte[] digest) {
		getBuilder().setDigestOfBlock(ByteString.copyFrom(digest));
	}
	
	public CommitMsg createCommitMsg() {
		CommitMsg commitMsg = new CommitMsg();
		commitMsg.setView(getView());
		commitMsg.setSeqNum(getSeqNum());
		commitMsg.setDigestOfBlock(getDigestOfBlock());
		return commitMsg;
	}

	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
	
	@Override
	public String toString() {
		return String.format("<Prepare, view: %d, seqNum: %d, d: %s>", 
				getView(), getSeqNum(), Hex.encode(getDigestOfBlock()));
	}
	
}
