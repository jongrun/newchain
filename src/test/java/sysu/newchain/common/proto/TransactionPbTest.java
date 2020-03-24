package sysu.newchain.common.proto;

import sysu.newchain.common.core.Address;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.TxMsgWithSignPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.TransactionPb;

public class TransactionPbTest {
	public static void main(String[] args) throws Exception {
		TransactionPb.Builder txBuilder = TransactionPb.newBuilder();
//		txBuilder.setAmount(10);
		TransactionPb txPb = txBuilder.build();
		System.out.println(txPb.getAmount());
		if (txPb.getFrom() == null) {
			System.out.println("hhh");			
		}
		if (txPb.getFrom().equals("")) {
			System.out.println("heheheh");
		}
		byte[] data = txPb.getData().toByteArray();
		System.out.println(data + " size: " + data.length);
		SchnorrKey ecKey = SchnorrKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		Transaction transaction = new Transaction(new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 20, "", null, ecKey.getPubKeyAsBytes(), "hahaha".getBytes());
		TxMsgWithSignPbCloner txCloner = (TxMsgWithSignPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.TRANSACTION);
		MsgWithSignPb tx = txCloner.toProto(transaction);
	}
}
