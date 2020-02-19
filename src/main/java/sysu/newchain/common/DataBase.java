package sysu.newchain.common;

import java.io.File;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.properties.AppConfig;

public class DataBase {
	private static final Logger logger = LoggerFactory.getLogger(DataBase.class);
	protected RocksDB rocksDB;
	String name;
	
	public DataBase(String name) {
		this.name = name;
	}
	
	public void init(){
		Options options = new Options();
		options.setCreateIfMissing(true);
		try {
			rocksDB = RocksDB.open(options, makeDbFile(name));
		} catch (RocksDBException e) {
			logger.error("", e);
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
}
