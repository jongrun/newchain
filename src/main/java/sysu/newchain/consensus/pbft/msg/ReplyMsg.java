package sysu.newchain.consensus.pbft.msg;

import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.proto.PrePreparePb;
import sysu.newchain.proto.ReplyPb;

public class ReplyMsg extends MsgBuilder<ReplyPb.Builder>{
	
	public ReplyMsg() {
		setBuilder(ReplyPb.newBuilder());
	}
	
	public ReplyMsg(byte[] data) throws InvalidProtocolBufferException {
		setBuilder(ReplyPb.parseFrom(data).toBuilder());
	}
	
	public long getView() {
		return getBuilder().getView();
	}
	
	public void setView(long view){
		getBuilder().setView(view);
	}
	
	public String getTime() {
		return getBuilder().getTime();
	}
	
	public void setTime(String time){
		getBuilder().setTime(time);
	}
	
	public long getReplica() {
		return getBuilder().getReplica();
	}
	
	public void setReplica(long replica){
		getBuilder().setReplica(replica);
	}
	
	public int getRetCode() {
		return getBuilder().getRetCode();
	}
	
	public void setRetCode(int retCode){
		getBuilder().setRetCode(retCode);
	}
	
	public String getRetMsg() {
		return getBuilder().getRetMsg();
	}
	
	public void setRetMsg(String retMsg){
		getBuilder().setRetMsg(retMsg);
	}
	
	public long getHeight() {
		return getBuilder().getHeight();
	}
	
	public void setHeight(long height){
		getBuilder().setHeight(height);
	}
	
	public String getBlockTime(){
		return getBuilder().getBlockTime();
	}
	
	public void setBlockTime(String blockTime){
		getBuilder().setBlockTime(blockTime);
	}
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
}
