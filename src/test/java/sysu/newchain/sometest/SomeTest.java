package sysu.newchain.sometest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.SerializationUtils;

import com.google.common.collect.Maps;

import sysu.newchain.common.format.Hex;


public class SomeTest {
	
	static Map<String, Status> statusMap = Maps.newConcurrentMap();
	
	enum Status{
		PREPARE,
		COMMIT
	}
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
		
		Status status = Status.PREPARE;
		if (statusMap.get("hello") == null) {
			System.out.println("isNull");
		}
		System.out.println(statusMap.get("hello"));
		System.out.println(status.equals(Status.COMMIT));
		try {
			System.out.println(statusMap.get("hello").equals(Status.COMMIT)); // Exception
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
		map.putIfAbsent("hh", 2);
		Integer a = map.computeIfAbsent("hh", k->3);
		Integer b = map.computeIfAbsent("xixi", k->4);
		
		map.computeIfPresent("hh", (k,v)->{
			if (v == 2) {
				return 5;
			}
			return null;
		});
		
		System.out.println(map.get("hh"));
		System.out.println(map.get("xixi"));
		System.out.println(a);
		System.out.println(b);
		System.out.println(Hex.encode(toByteArray(12)));
		System.out.println(Hex.encode(SerializationUtils.serialize(12)));
	}
	
	public static byte[] toByteArray (Object obj) {
		SerializationUtils.serialize(1);
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();      
        try {        
            ObjectOutputStream oos = new ObjectOutputStream(bos);         
            oos.writeObject(obj);        
            oos.flush();         
            bytes = bos.toByteArray ();      
            oos.close();         
            bos.close();        
        } catch (IOException ex) {        
            ex.printStackTrace();   
        }      
        return bytes;    
    }
}
