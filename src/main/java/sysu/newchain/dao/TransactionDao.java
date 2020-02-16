package sysu.newchain.dao;

import java.io.IOException;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.DataBase;
import sysu.newchain.common.format.Hex;
import sysu.newchain.core.Transaction;

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
//			logger.debug("insert value: {}", Hex.encode(value));
			rocksDB.put(tx.calculateHash(), value);
		} catch (RocksDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
