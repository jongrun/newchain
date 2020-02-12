package sysu.newchain.common.proto;

import com.google.protobuf.ByteString;

import sysu.newchain.core.Address;
import sysu.newchain.core.Transaction;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.TransactionPb;

public class TransactionPbCloner implements ProtoCloner<Transaction, MsgWithSignPb> {

	@Override
	public Transaction toObject(MsgWithSignPb p) {
		TransactionPb tx = p.getTxMsg();
		try {
			Transaction transaction = new Transaction(
					new Address(tx.getFrom()), 
					new Address(tx.getTo()), 
					tx.getAmount(),
					tx.getTime(),
					tx.getSign().toByteArray(),
					tx.getPubKey().toByteArray(),
					tx.getData().toByteArray(),
					tx.getClientPubKey().toByteArray());
			transaction.setClientSign(p.getSign().toByteArray());
			return transaction;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public MsgWithSignPb toProto(Transaction o) {
		MsgWithSignPb.Builder msgBuilder = MsgWithSignPb.newBuilder();
		TransactionPb.Builder txBuilder = TransactionPb.newBuilder();
		txBuilder.setFrom(o.getFrom().getEncodedBase58());
		txBuilder.setTo(o.getTo().getEncodedBase58());
		txBuilder.setAmount(o.getAmount());
		txBuilder.setTime(o.getTime());
		txBuilder.setSign(ByteString.copyFrom(o.getSign()));
		txBuilder.setPubKey(ByteString.copyFrom(o.getPubKey()));
		txBuilder.setData(ByteString.copyFrom(o.getData()));
		txBuilder.setHash(ByteString.copyFrom(o.getHash()));
		txBuilder.setClientPubKey(ByteString.copyFrom(o.getClientPubKey()));
		msgBuilder.setTxMsg(txBuilder);
		msgBuilder.setSign(ByteString.copyFrom(o.getClientSign()));
		return msgBuilder.build();
	}

}
