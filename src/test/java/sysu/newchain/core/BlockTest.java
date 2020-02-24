package sysu.newchain.core;

import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;

public class BlockTest {
	public static void main(String[] args) throws Exception {
		BlockHeader header = new BlockHeader();
		header.setHeight(1L);
		header.setTime("");
		header.setPrehash(Hash.SHA256.hashTwice("hello".getBytes()));
		
		Block block = new Block();
		block.setHeader(header);
		SchnorrKey ecKey = SchnorrKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		Transaction transaction = new Transaction(new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 20, "", null, ecKey.getPubKeyAsBytes(), "hahaha".getBytes());
		byte[] sign = ecKey.sign(transaction.getHash()).toByteArray();
		transaction.setSign(sign);
		byte[] txBytes = transaction.serialize();
		
		Transaction transaction2 = new Transaction(txBytes);
		
		block.addTransaction(transaction);
		block.addTransaction(transaction2);
		block.calculateAndSetHash();
		
		byte[] blockBytes = block.serialize();
		
		Block block2 = new Block(blockBytes);
		
		String blockhash = Hex.encode(block.getHash());
		String block2Hash = Hex.encode(block2.getHash());
		System.out.println(blockhash.equals(block2Hash));
		System.out.println(block2Hash.equals(Hex.encode(block2.calculateHash())));
		System.out.println(blockhash);
	}
}
