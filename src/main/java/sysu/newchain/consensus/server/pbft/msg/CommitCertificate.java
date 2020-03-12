package sysu.newchain.consensus.server.pbft.msg;

import java.util.List;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.proto.CommitCertificatePb;
import sysu.newchain.proto.PrepareCertificatePb;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class CommitCertificate extends MsgBuilder<CommitCertificatePb.Builder>{
	public CommitCertificate() {
		setBuilder(CommitCertificatePb.newBuilder());
	}
	
	public CommitCertificate(byte[] data) throws InvalidProtocolBufferException{
		setBuilder(CommitCertificatePb.parseFrom(data).toBuilder());
	}
	
	public CommitMsg getCommitMsg(){
		CommitMsg commitMsg = new CommitMsg();
		commitMsg.setBuilder(getBuilder().getCommitMsgBuilder());
		return commitMsg;
	}
	
	public void setCommitMsg(CommitMsg commitMsg) {
		getBuilder().setCommitMsg(commitMsg.getBuilder());
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
		return SchnorrKey.verifyMulSig(Hash.SHA256.hash(getCommitMsg().toByteArray()), getSign(), pubKeys);
	}
}
