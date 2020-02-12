package sysu.newchain.dao;

import java.io.File;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import sysu.newchain.properties.AppConfig;

public class DataBase {
	protected RocksDB rocksDB;
	
	public DataBase(String dbName) {
		Options options = new Options();
		options.setCreateIfMissing(true);
		try {
			rocksDB = RocksDB.open(options, makeDbFile(dbName));
		} catch (RocksDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String makeDbFile(String dbName) {
		String dbDir = System.getProperty("user.dir").trim() + File.separator + AppConfig.getDataDir() + File.separator + dbName;
		File dbFile = new File(dbDir);
		if(!dbFile.exists()) {
			dbFile.mkdirs();
		}
		return dbDir;
	}
	
	public static void main(String[] args) {
		DataBase dataBase = new DataBase("test");
	}
}
