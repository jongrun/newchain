package sysu.newchain.consensus.server.pbft.msg;

import java.nio.charset.Charset;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.proto.BlockPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.common.proto.TransactionPbCloner;
import sysu.newchain.core.Block;
import sysu.newchain.core.Transaction;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.MsgWithSignPb.MsgCase;
import sysu.newchain.proto.TransactionPb;


/**
 * @Description Contains a message with its signature
 * @author jongliao
 * @date 2020年2月6日 下午5:44:03
 */
public class MsgWithSign extends MsgBuilder<MsgWithSignPb.Builder> implements Signable{
	
	public MsgWithSign() {
		setBuilder(MsgWithSignPb.newBuilder());
	}
	
	public MsgWithSign(byte[] data) throws InvalidProtocolBufferException {
		setBuilder(MsgWithSignPb.parseFrom(data).toBuilder());
	}
	
	public MsgWithSign(Transaction tx) {
		TransactionPbCloner transactionPbCloner = (TransactionPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.TRANSACTION);
		MsgWithSignPb txMsgWithSignPb = transactionPbCloner.toProto(tx);
		setBuilder(txMsgWithSignPb.toBuilder());
	}
	
	public Transaction toTransaction() {
		TransactionPbCloner transactionPbCloner = (TransactionPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.TRANSACTION);
		return transactionPbCloner.toObject(getBuilder().build());
	}
	
	public static MsgWithSign fromTransaction(Transaction tx) {
		return new MsgWithSign(tx);
	}
	
	public MsgCase getMsgCase(){
		return getBuilder().getMsgCase();
	}
	
	public TxMsg getTxMsg() {
		TxMsg txMsg = new TxMsg();
		txMsg.setBuilder(getBuilder().getTxMsgBuilder());
		return txMsg;
	}
	
	public void setTxMsg(TxMsg txMsg) {
		getBuilder().setTxMsg(txMsg.getBuilder());
	}
	
	public BlockMsg getBlockMsg() {
		BlockMsg blockMsg = new BlockMsg();
		blockMsg.setBuilder(getBuilder().getBlockMsgBuilder());
		return blockMsg;
	}
	
	public void setBlockMsg(BlockMsg blockMsg) {
		getBuilder().setBlockMsg(blockMsg.getBuilder());
	}
	
	public void setBlock(Block block) {
		BlockMsg blockMsg = new BlockMsg(block);
		setBlockMsg(blockMsg);
	}
	
	public PrePrepareMsg getPrePrepareMsg() {
		PrePrepareMsg prePrepare = new PrePrepareMsg();
		prePrepare.setBuilder(getBuilder().getPrePrepareMsgBuilder());
		return prePrepare;
	}
	
	public void setPrePrepareMsg(PrePrepareMsg prePrepareMsg) {
		getBuilder().setPrePrepareMsg(prePrepareMsg.getBuilder());
	}
	
	public PrepareMsg getPrepareMsg(){
		PrepareMsg prepare = new PrepareMsg();
		prepare.setBuilder(getBuilder().getPrepareMsgBuilder());
		return prepare;
	}
	
	public void setPrepareMsg(PrepareMsg prepareMsg) {
		getBuilder().setPrepareMsg(prepareMsg.getBuilder());
	}
	
	public CommitMsg getCommitMsg() {
		CommitMsg commitMsg = new CommitMsg();
		commitMsg.setBuilder(getBuilder().getCommitMsgBuilder());
		return commitMsg;
	}
	
	public void setCommitMsg(CommitMsg commitMsg) {
		getBuilder().setCommitMsg(commitMsg.getBuilder());
	}
	
	public ReplyMsg getReplyMsg(){
		ReplyMsg replyMsg = new ReplyMsg();
		replyMsg.setBuilder(getBuilder().getReplyMsgBuilder());
		return replyMsg;
	}
	
	public void setReplyMsg(ReplyMsg replyMsg) {
		getBuilder().setReplyMsg(replyMsg.getBuilder());
	}
	
	public byte[] getSign() {
		return getBuilder().getSign().toByteArray();
	}
	
	public void setSign(byte[] sign) {
		getBuilder().setSign(ByteString.copyFrom(sign));
	}
	
	public byte[] toByteArray() {
		return getBuilder().build().toByteArray();
	}
	
	@Override
	public String toString() {
		return new String(this.toByteArray(), Charset.forName("ISO-8859-1"));
	}
	
	public byte[] getBytesToSign() throws Exception {
		return Hash.SHA256.hash(getBytesToHash());
	}
	
	public byte[] getBytesToHash() throws Exception {
		switch (getBuilder().getMsgCase()) {
		case TXMSG:
			return getBuilder().getTxMsg().toByteArray();
		case REPLYMSG:
			return getBuilder().getReplyMsg().toByteArray();
		case PREPREPAREMSG:
			return getBuilder().getPrePrepareMsg().toByteArray();
		case PREPAREMSG:
			return getBuilder().getPrepareMsg().toByteArray();
		case COMMITMSG:
			return getBuilder().getCommitMsg().toByteArray();
		case BLOCKMSG:
			return getBuilder().getBlockMsg().toByteArray();
		case MSG_NOT_SET:
		default:
			throw new Exception("MSG_NOT_SET");
		}
	}
	
	@Override
	public byte[] calculateSign(SchnorrKey ecKey) throws Exception{
		return ecKey.sign(getBytesToSign()).toByteArray();
	}
	
	@Override
	public void calculateAndSetSign(SchnorrKey ecKey) throws Exception {
		setSign(calculateSign(ecKey));
	}
	
	@Override
	public boolean verifySign(byte[] pubKey) {
		SchnorrKey ecKey = SchnorrKey.fromPubKeyOnly(pubKey);
		try {
			return ecKey.verify(getBytesToSign(), getSign());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
}
