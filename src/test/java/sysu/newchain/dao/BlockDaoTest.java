package sysu.newchain.dao;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.core.Address;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;

public class BlockDaoTest {
	static final Logger logger = LoggerFactory.getLogger(BlockDaoTest.class);
	
	public static void main(String[] args) throws Exception {
		// gen block
		BlockHeader header = new BlockHeader();
		header.setHeight(1L);
		header.setTime("time123");
		header.setPrehash(Hash.SHA256.hashTwice("hello".getBytes()));
		Block block = new Block();
		block.setHeader(header);
		ECKey ecKey = ECKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		
		Transaction transaction = new Transaction(
				new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 
				new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 
				20, 
				"time", 
				null, 
				ecKey.getPubKeyAsBytes(), 
				"hahaha".getBytes());
		transaction.calculateAndSetSign(ecKey);
//		logger.debug(transaction.toString());
		block.addTransaction(transaction);
		block.calculateAndSetHash();
		
		// tx dao
		TransactionDao txDao = TransactionDao.getInstance();
		txDao.insertTransaction(transaction);
		
		Transaction transaction2 = txDao.getTransaction(transaction.getHash());
		logger.debug("tx hash: {}", Hex.encode(transaction.getHash()));
		logger.debug("tx2 hash£º {}", Hex.encode(transaction2.getHash()));
//		logger.debug(transaction2.toString());
		
		// block dao
		BlockDao blockDao = BlockDao.getInstance();
		blockDao.setLastHeight(1L);
		blockDao.insertBlock(block);
		logger.debug("height: {}", blockDao.getLastHeight());
		Block block2 = blockDao.getBlock(1L);
		logger.debug("block hash: {}", Hex.encode(block.getHash()));
		logger.debug("block2 hash: {}", Hex.encode(block2.getHash()));
		logger.debug("block2 calculate hash: {}", Hex.encode(block2.calculateHash()));
	}
}
