package sysu.newchain.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/** 追踪交易请求收到的响应。
 * 每个交易请求由交易hash唯一标识，记录每个请求收到的响应值 V 的发送方 T（投票方），
 * 当某响应值 V 值投票数量达到 majority 时，add()返回 true，
 * 此时外部调用notifyAndRemove完成响应并移除该请求，后续收到该请求的响应不再做处理
 * @author jongliao
 * @date: 2020年2月17日 下午12:54:26
 * @param <T> 投票方
 * @param <V> 投票值
 */
public class RequestVoteTable<T, V> {
	static final Logger logger = LoggerFactory.getLogger(RequestVoteTable.class);
	
	protected final Map<String, Entry<T, V>> requests = new ConcurrentHashMap<String, Entry<T, V>>(); 
	
	
	
	public Entry<T, V> get(String index) {
		return requests.get(index);
	}
	
	public void create(String index, CompletableFuture future) {
		Entry<T, V> entry = new Entry<T, V>(future);
		requests.putIfAbsent(index, entry);
	}
	
	/**
	 * Adds a response to the response set. If the majority has been reached,
	 * returns true
	 * 
	 * @return True if a majority has been reached, false otherwise. Note that this
	 *         is done <em>exactly once</em>
	 */
	public boolean add(String index, T vote, V value, int majority) {
		requests.computeIfPresent(index, (k, v)->{
			logger.debug("add index: {}, vote: {}, majority: {}", index, vote, majority);
			v.add(vote, value, majority);
			logger.debug("add success");
			return v;
		});
		return isCommitted(index);
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

	/** Notifies the CompletableFuture and then removes the entry for index */
	public  void notifyAndRemove(String index, Object response) {
		requests.computeIfPresent(index, (k, v)->{
			if (v.resultFuture != null) {
				v.resultFuture.complete(response);
			}
			return null;
		});
	}

	/** Removes the entry for index 
	 * @param index
	 */
	public  void remove(String index) {
		requests.computeIfPresent(index, (k, v)->{
			return null;
		});
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Entry<T, V>> entry : requests.entrySet())
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		return sb.toString();
	}

	/**
	 * @author jongliao
	 * @date: 2020年2月17日 下午12:28:53
	 * @param <T> 投票方
	 * @param <V> 投票值
	 */
	public static class Entry<T, V> {
		Logger logger = LoggerFactory.getLogger(Entry.class);
		
		// the future has been returned to the caller, and needs to be notified when
		// we've reached a majority
		protected final CompletableFuture<Object> resultFuture;
		
		// voteValue -> voters
		protected Map<V, Set<T>> votes = Maps.newConcurrentMap();
		protected Boolean committed = false;
		
		public Entry(CompletableFuture<Object> resultFuture) {
			this.resultFuture = resultFuture;
		}

		protected boolean add(T vote, V value, int majority) {
			votes.compute(value, (k, v) ->{
				if (v == null) {
					v = new HashSet<T>();
				}
				logger.debug("before votes: {}", v);
				boolean success = v.add(vote);
				if (v.size() >= majority) {
					committed = true;
				}
				logger.debug("after votes: {}", v);
				return v;
			});
			logger.debug("committed: {}", committed);
			return committed;
		}

		public CompletableFuture<?> getResultFuture() {
			return resultFuture;
		}

		@Override
		public String toString() {
			return "committed=" + committed + ", votes=" + votes;
		}
	}
}
