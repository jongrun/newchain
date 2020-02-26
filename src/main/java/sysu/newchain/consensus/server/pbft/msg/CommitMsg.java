package sysu.newchain.consensus.server.pbft.msg;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.format.Hex;
import sysu.newchain.proto.CommitPb;

public class CommitMsg extends MsgBuilder<CommitPb.Builder>{
	public CommitMsg() {
		setBuilder(CommitPb.newBuilder());
	}
	
	public CommitMsg(byte[] data) throws InvalidProtocolBufferException{
		setBuilder(CommitPb.parseFrom(data).toBuilder());
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
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
	
	@Override
	public String toString() {
		return String.format("<Commit, view: %d, seqNum: %d, d: %s>", 
				getView(), getSeqNum(), Hex.encode(getDigestOfBlock()));
	}
	
}
