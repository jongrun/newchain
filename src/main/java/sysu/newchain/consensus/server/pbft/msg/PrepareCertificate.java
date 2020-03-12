package sysu.newchain.consensus.server.pbft.msg;

import java.util.List;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.crypto.SchnorrKey.SchnorrSignature;
import sysu.newchain.proto.PrepareCertificatePb;
import sysu.newchain.proto.PreparePb;

public class PrepareCertificate extends MsgBuilder<PrepareCertificatePb.Builder>{
	
	public PrepareCertificate() {
		setBuilder(PrepareCertificatePb.newBuilder());
	}
	
	public PrepareCertificate(byte[] data) throws InvalidProtocolBufferException{
		setBuilder(PrepareCertificatePb.parseFrom(data).toBuilder());
	}
	
	public PrepareMsg getPrepareMsg(){
		PrepareMsg prepareMsg = new PrepareMsg();
		prepareMsg.setBuilder(getBuilder().getPrepareMsgBuilder());
		return prepareMsg;
	}
	
	public void setPrepareMsg(PrepareMsg prepareMsg){
		getBuilder().setPrepareMsg(prepareMsg.getBuilder());
	}
	
	public List<Long> getReplicaList() {
		return getBuilder().getReplicaList();
	}
	
	public void setReplicaList(List<Long> replicaList){
		getBuilder().addAllReplica(replicaList);
	}
	
	public byte[] getSign(){
		return getBuilder().getSign().toByteArray();
	}
	
	public void setSign(byte[] sign){
		getBuilder().setSign(ByteString.copyFrom(sign));
	}
	
	@Override
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
	
	public boolean verifyMulSig(List<byte[]> pubKeys){
		return SchnorrKey.verifyMulSig(Hash.SHA256.hash(getPrepareMsg().toByteArray()), getSign(), pubKeys);
	}

}
