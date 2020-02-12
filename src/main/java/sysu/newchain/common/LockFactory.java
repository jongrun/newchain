package sysu.newchain.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockFactory {
	private final static ConcurrentMap<String , ReentrantLock> lockMap = new ConcurrentHashMap<String, ReentrantLock>();
	
	public static ReentrantLock getLock(String key) {
		ReentrantLock lock = lockMap.get(key);
		if (lock == null) {
			lock = new ReentrantLock();
			ReentrantLock temp = lockMap.putIfAbsent(key, lock);
			if (temp != null) {
				lock = temp;
			}
		}
		return lock;
	}
}
