package sysu.newchain.sometest;

import java.util.HashMap;
import java.util.Map;

public class SomeTest {
	public static void main(String[] args) {
		System.out.println(String.format("hello%d", 100L));
		System.out.println(System.getProperty("--spring.config.location"));
		System.out.println(args[0]);
		
		Map<Long, String> hashMap = new HashMap<Long, String>();
		long key = 100;
		hashMap.put(key, "hello");
		if (hashMap.containsKey(key)) {
			System.out.println(hashMap.get(key));
		}
	}
}
