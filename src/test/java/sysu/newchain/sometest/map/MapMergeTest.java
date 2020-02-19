package sysu.newchain.sometest.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class MapMergeTest {
	public static void main(String[] args) {
		Map<Integer, Set<Long>> map = Maps.newConcurrentMap();
		System.out.println(map.get(5));
		map.merge(5, new HashSet<Long>(){{add(0L);}}, (k,v)->{
			System.out.println(v);
			v.add(0L);
			System.out.println(v);
			return v;
		});
		System.out.println(map.get(5));
		map.merge(5, new HashSet<Long>(){{add(1L);}}, (old,v)->{
			System.out.println(v);
			v.add(10L);
			System.out.println(v);
			return v;
		});
		System.out.println(map.get(5));
		map.merge(5, new HashSet<Long>(){{add(2L);}}, (k,v)->{
			System.out.println(v);
			v.add(2L);
			System.out.println(v);
			return v;
		});
		System.out.println(map.get(5));
	}
}
