package sysu.newchain.sometest.map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sysu.newchain.common.format.Utils;

import com.google.common.collect.Maps;

public class MapRemoveTest {
	public static void main(String[] args) {
		Map<byte[], byte[]> map = Maps.newConcurrentMap();
		
		map.put("n_1_v_1".getBytes(), Utils.longToBytes(20));
		map.put("n_1_v_2".getBytes(), Utils.longToBytes(21));
		map.put("n_2_v_1".getBytes(), Utils.longToBytes(22));
		map.put("n_2_v_2".getBytes(), Utils.longToBytes(23));
		map.put("n_3_v_2".getBytes(), Utils.longToBytes(24));
		map.put("n_4_v_2".getBytes(), Utils.longToBytes(25));
		
		for (byte[] key : map.keySet()) {
			String keyString = new String(key);
			long valueLong = Utils.bytesToLong(map.get(key));
			System.out.format("key: %s, value: %d%n", keyString, valueLong);
//			if (keyString.startsWith("n_2") || keyString.startsWith("n_1")) {
////				System.out.println("remove");
//				map.remove(key);
//			}
		}
		
		System.out.println();
		
		for(Iterator<Map.Entry<byte[], byte[]>> iterator = map.entrySet().iterator(); iterator.hasNext();){
			Map.Entry<byte[], byte[]> it = iterator.next();
			String keyString = new String(it.getKey());
			long valueLong = Utils.bytesToLong(it.getValue());
			System.out.format("key: %s, value: %d%n", keyString, valueLong);
			if (keyString.startsWith("n_2") || keyString.startsWith("n_1")) {
				System.out.println("remove");
				map.remove(it.getKey());
			}
			Map.Entry<byte[], byte[]> it2 = iterator.next();
			String keyString2 = new String(it2.getKey());
			long valueLong2 = Utils.bytesToLong(it2.getValue());
			System.out.format("key: %s, value: %d%n", keyString2, valueLong2);
		}
		
		System.out.println();
		
		for (byte[] key : map.keySet()) {
			String keyString = new String(key);
			long valueLong = Utils.bytesToLong(map.get(key));
			System.out.format("key: %s, value: %d%n", keyString, valueLong);
		}
		
//		
//		System.out.println();
//		
//		for (Integer value : map.values()) {
//			System.out.format("value: %d%n", value);
//		}
//		
//		System.out.println();
//		
//		for (Map.Entry<String, Integer> entry : map.entrySet()) {
//			System.out.format("key: %s, value: %d%n", entry.getKey(), entry.getValue());
//		}
//		
//		System.out.println();
//		
//		for(Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator(); iterator.hasNext();){
//			Map.Entry<String, Integer> it = iterator.next();
//			System.out.format("key: %s, value: %d%n", it.getKey(), it.getValue());
//			System.out.println(it);
//		}
//		
//		System.out.println();
//		
//		map.forEach((k, v)->{
//			System.out.format("key: %s, value: %d%n", k, v);
//		});
//		
//		System.out.println();
		
	}
}
