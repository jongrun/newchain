package sysu.newchain.dao;

import java.util.concurrent.locks.Lock;

import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.rocksdb.RocksDBException;

import sysu.newchain.common.DataBase;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Utils;
import sysu.newchain.core.Address;

/**
 * @Description TODO
 * @author jongliao
 * @date 2020年2月4日 下午4:22:48
 */
public class AccountDao extends DataBase{
	static final String DBNAME = "account.db";
	
	private static final AccountDao instance = new AccountDao();
	
	public static AccountDao getInstance() {
		return instance;
	}
	
	private AccountDao() {
		super(DBNAME);
	}
	
	public void setBalance(Address address, long balance) throws Exception {
		byte[] key = Base58.decode(address.getEncodedBase58());
		rocksDB.put(key, Utils.longToBytes(balance));
	}
	
	public long getBalance(Address address) throws RocksDBException, Exception {
		byte[] key = Base58.decode(address.getEncodedBase58());
		byte[] value = rocksDB.get(key);
		if (value != null) {
			return Utils.bytesToLong(value);
		}
		return 0L;
	}
	
	private boolean withdraw(Address address, long amount) throws RocksDBException, Exception {
		long balance = getBalance(address);
		if (amount > balance) {
			return false;
		}
		else {
			balance = balance - amount;
			setBalance(address, balance);
			return true;
		}
	}
	
	private void deposit(Address address, long amount) throws Exception {
		long balance = getBalance(address);
		balance += amount;
		setBalance(address, balance);
	}
	
	public boolean transfer(Address from, Address to, long amount) throws RocksDBException, Exception {
		if (withdraw(from, amount)) {
			deposit(to, amount);
		}
		return false;
	}
	
}
