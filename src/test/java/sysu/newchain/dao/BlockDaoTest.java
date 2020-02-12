package sysu.newchain.dao;

import org.rocksdb.RocksDBException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.core.Address;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;

public class BlockDaoTest {
	public static void main(String[] args) throws Exception {
		// gen block
		BlockHeader header = new BlockHeader();
		header.setHeight(1L);
		header.setTime("");
		header.setPrehash(Hash.SHA256.hashTwice("hello".getBytes()));
		Block block = new Block();
		block.setHeader(header);
		ECKey ecKey = ECKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		Transaction transaction = new Transaction(new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 20, "", null, ecKey.getPubKeyAsBytes(), "hahaha".getBytes());
		byte[] sign = ecKey.sign(transaction.getHash()).encodeToDER();
		transaction.setSign(sign);
		block.addTransaction(transaction);
		block.calculateAndSetHash();
		
		// tx dao
		TransactionDao txDao = new TransactionDao();
		txDao.insertTransaction(transaction);
		Transaction transaction2 = txDao.getTransaction(transaction.getHash());
		System.out.println(Hex.encode(transaction.getHash()));
		System.out.println(Hex.encode(transaction2.getHash()));
		
		// block dao
		BlockDao blockDao = new BlockDao();
		blockDao.setLastHeight(1L);
		blockDao.insertBlock(block);
		System.out.println(blockDao.getLastHeight());
		Block block2 = blockDao.getBlock(1L);
		System.out.println(Hex.encode(block.getHash()));
		System.out.println(Hex.encode(block2.getHash()));
		System.out.println(Hex.encode(block2.calculateHash()));
	}
}
