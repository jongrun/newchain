package sysu.newchain.sometest.map;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class MapTest {
	public static void main(String[] args) {
		Map<String, Object> map = new ConcurrentHashMap<String, Object>();
		map.put("safein", "safein");
		map.put("safein1tebg23", "safein23");
		map.put("twjsafein123", "twjsafein1233");
		map.put("twj", "twj");
		List<?> list = getLikeByMap(map, "twj");
		for (Object val : list) {
			System.err.println(val.toString());
		}
	}

	public static List<String> getLikeByMap(Map<String, Object> map,
			String keyLike) {
		List<String> list = new Vector<>();
		for (Map.Entry<String, Object> entity : map.entrySet()) {
			if (entity.getKey().indexOf(keyLike) > -1) {
				list.add((String) entity.getValue());
			}
		}
		return list;
	}
}