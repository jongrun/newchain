package sysu.newchain.dao;

import java.io.File;
import java.io.IOException;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import sysu.newchain.common.format.Utils;
import sysu.newchain.core.Block;

public class BlockDao extends DataBase{
	static final String DBNAME = "block.db";
	
	public BlockDao() {
		super(DBNAME);
	}
	
	public void insertBlock(Block block) throws Exception {
		rocksDB.put(Utils.longToBytes(block.getHeader().getHeight()), block.serialize());
	}
	
	public Block getBlock(long height) throws Exception {
		byte[] value = rocksDB.get(Utils.longToBytes(height));
		if (value != null) {
			return new Block(value);
		}
		return null;
	}
	
	public void setLastHeight(long height) throws RocksDBException {
		rocksDB.put(Utils.longToBytes(-1), Utils.longToBytes(height));
	}
	
	public long getLastHeight() throws RocksDBException {
		byte[] value = rocksDB.get(Utils.longToBytes(-1));
		if (value != null) {
			return Utils.bytesToLong(value);
		}
		else {
			return -1L;
		}
	}
}
