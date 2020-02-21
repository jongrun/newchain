package sysu.newchain.consensus.server.pbft.msg;

import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.proto.TransactionPb;

public class TxMsg extends MsgBuilder<TransactionPb.Builder>{

	public TxMsg() {
		setBuilder(TransactionPb.newBuilder());
	}
	
	public TxMsg(byte[] data) throws InvalidProtocolBufferException{
		setBuilder(TransactionPb.parseFrom(data).toBuilder());
	}
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
	
}
