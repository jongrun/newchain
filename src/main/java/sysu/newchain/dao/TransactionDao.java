package sysu.newchain.dao;

import java.io.IOException;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.DataBase;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.format.Hex;

public class TransactionDao extends DataBase{
	static final Logger logger = LoggerFactory.getLogger(TransactionDao.class);
	
	static final String DBNAME = "transaction.db";
	
	private static final TransactionDao instance = new TransactionDao();
	
	public static TransactionDao getInstance() {
		return instance;
	}
	
	private TransactionDao() {
		super(DBNAME);
	}
	
	public void insertTransaction(Transaction tx) throws Exception {
		try {
			byte[] value = tx.serialize();
			rocksDB.put(tx.calculateHash(), value);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public Transaction getTransaction(byte[] hash) throws Exception {
		byte[] value = rocksDB.get(hash);
		if (value != null) {
			return new Transaction(value);
		}
		else {
			return null;
		}
	}
}
