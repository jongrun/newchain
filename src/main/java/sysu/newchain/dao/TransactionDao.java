package sysu.newchain.dao;

import java.io.IOException;

import org.rocksdb.RocksDBException;

import sysu.newchain.common.DataBase;
import sysu.newchain.core.Transaction;

public class TransactionDao extends DataBase{
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
			rocksDB.put(tx.getHash(), tx.serialize());
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
