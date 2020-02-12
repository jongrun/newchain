package sysu.newchain.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of Request messages and responses. Each request is keyed by the
 * trasid at which it was inserted at the leader. The values (RequestEntry)
 * contain the responses. When a response is added, and the majority has been
 * reached, add() returns true and the key/value pair will be removed.
 * (subsequent responses will be ignored).
 */
public class RequestVoteTable<T, V> {
	protected final Map<String, Entry<T, V>> requests = new ConcurrentHashMap<String, Entry<T, V>>(); 

	private final Map<String, Long> indexLock = new ConcurrentHashMap<String, Long>(); 
	private AtomicLong atomicLong = new AtomicLong(0);

	public Entry<T, V> get(String index) {
		return requests.get(index);
	}

	public void create(String index, CompletableFuture future) {
		Entry<T, V> entry = new Entry<T, V>(future);
		requests.put(index, entry);
	}



	/**
	 * Adds a response to the response set. If the majority has been reached,
	 * returns true
	 * 
	 * @return True if a majority has been reached, false otherwise. Note that this
	 *         is done <em>exactly once</em>
	 */
	public boolean add(String index, T vote, V value, int majority) {
		Entry<T, V> entry = requests.get(index);
		if (entry == null)
			return false;

		return entry.add(vote, value, majority);
	}

	/** Whether or not the entry at index is committed */
	public synchronized boolean isCommitted(String index) {
		Entry<T, V> entry = requests.get(index);
		return entry != null && entry.committed;
	}

	/** number of requests being processed */
	public synchronized int size() {
		return requests.size();
	}

//	public synchronized int votes(String index, V value) {
//		Entry<T, V> entry = requests.get(index);
//		if (entry == null)
//			return 0;
//		Set<T> valueVotes = entry.votes.get(value);
//		return valueVotes != null ? valueVotes.size() : 0;
//	}

	/** Notifies the CompletableFuture and then removes the entry for index */
	public  void notifyAndRemove(String index, Object response) {
		Entry<T, V> entry = requests.get(index);
		if (entry != null) {
			if (entry.resultFuture != null) {
				entry.resultFuture.complete(response);
			}
			requests.remove(index);
		}
	}

	public  void remove(String index) {
		Entry<T, V> entry = requests.get(index);
		if (entry != null) {
			requests.remove(index);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Entry<T, V>> entry : requests.entrySet())
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		return sb.toString();
	}

	public static class Entry<T, V> {
		// the future has been returned to the caller, and needs to be notified when
		// we've reached a majority
		protected final CompletableFuture resultFuture;
		protected final Map<V, AtomicInteger> votes = new HashMap<V, AtomicInteger>();
		protected Boolean committed = false;
		
		protected AtomicInteger result0 = new AtomicInteger(0);
		protected AtomicInteger result1 = new AtomicInteger(0);



		public Entry(CompletableFuture resultFuture) {
			this.resultFuture = resultFuture;
		}

		protected boolean add(T vote, V value, int majority) {
			
			if (!votes.containsKey(value)) {
				votes.put(value, new AtomicInteger(0));
			}
//			votes.get(value).incrementAndGet();
//			for (V key : votes.keySet()) {
//				if (votes.get(key).get() >= majority) {
//					if (!committed) {
//						committed = true;
//						return true;
//					}
//				}
//			}
			if (votes.get(value).incrementAndGet() >= majority) {
				if (!committed) {
					committed = true;
					return true;
				}
			}
//			if(value == (Integer)0) {
//				int c = result0.incrementAndGet();
//				if(c>= majority){
//					//synchronized(committed) 
//					{
//						if (!committed) {
//							//votes.put(value, new AtomicInteger(c));
//							committed = true;
//							return true;
//						}
//					}
//				}
//			}else  {
//				int c = result1.incrementAndGet();
//				if(c>= majority){
//					//synchronized(committed) 
//					{
//						if (!committed) {
//							//votes.put(value, new AtomicInteger(c));
//							committed = true;
//							return true;
//						}
//					}
//				}
//			}
			return false;
		}

		public CompletableFuture<Object> getResultFuture() {
			return resultFuture;
		}

		@Override
		public String toString() {
			return "committed=" + committed;// + ", votes=" + votes;
		}
	}
}
