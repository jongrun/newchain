package sysu.newchain.common;

import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.rocksdb.RocksDBException;

import com.google.common.collect.Maps;

public class ConcurrentKV <K, V> extends DataBase{
	private ConcurrentMap<byte[], byte[]> map = Maps.newConcurrentMap();

	public ConcurrentKV(String dbName) {
		super(dbName);
	}
	
	public byte[] get(byte[] key) throws RocksDBException {
		return rocksDB.get(key);
	}
	
	public byte[] put(byte[] key, byte[] value) throws RocksDBException {
		rocksDB.put(key, value);
		return map.put(key, value);
	}
	
	public byte[] putIfAbsent(byte[] key, byte[] value) {
//		map.compute(key, remappingFunction);
//		map.computeIfAbsent(key, mappingFunction);
//		map.computeIfPresent(, remappingFunction);
		return map.putIfAbsent(key, value);
	}
	
//	public byte[] compute(byte[] key, BiFunction<byte[], byte[], byte[]> remappingFunction) {
//		
//	}
	
}
