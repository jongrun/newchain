package sysu.newchain.common.proto;

public class ProtoClonerFactory {
	
	private static TxMsgWithSignPbCloner transactionPbCloner = new TxMsgWithSignPbCloner();
	private static BlockPbCloner blockPbCloner = new BlockPbCloner();
	
	public static ProtoCloner getCloner(ProtoClonerType type) {
		switch (type) {
		case TRANSACTION:
			return transactionPbCloner;
		case BLOCK:
			return blockPbCloner;
		default:
			return null;
		}
	}
	
	public static enum ProtoClonerType{
		TRANSACTION,
		BLOCK
	}
}
