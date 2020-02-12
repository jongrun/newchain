package sysu.newchain.common;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description 用于创建线程池
 * @author jongliao
 * @date 2020年1月20日 上午10:31:12
 */
/**
 * @Description TODO
 * @author jongliao
 * @date 2020年1月20日 上午10:32:32
 */
public class ThreadUtil {

	public static ExecutorService createExecutorService(String name, Integer size, boolean isPreStart) {
		ExecutorService executorService = Executors.newFixedThreadPool(size, new ThreadFactory() {
			private AtomicInteger counter = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName(name + "-" + counter.getAndIncrement());
				return t;
			}
		});
		if (isPreStart)
			((ThreadPoolExecutor) executorService).prestartAllCoreThreads();

		return executorService;
	}

	/**
	 * @Description: TODO
	 * @param name
	 * @param size
	 * @param isPreStart
	 * @return
	 */
	public static ThreadPoolExecutor createThreadPool(String name, Integer size, boolean isPreStart) {
		ThreadPoolExecutor tp = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(2048, true), new ThreadFactory() {
					private AtomicInteger counter = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName(name + counter.getAndIncrement());
						return t;
					}
				});

		if (isPreStart)
			tp.prestartAllCoreThreads();

		return tp;
	}
	
	/**
	 * Safe Thread Pool ( !!! add queue max size avoid OOM  and fair lock for wait thread!!!)
	 * 
	 * @param name           threadPool Name
	 * @param threadCount    threadPool Core Thread Count
	 * @param queueSize      threadPool Queue Max Size
	 * @param isPreStart     threadPool is preStart
	 * @return
	 */
	public static ThreadPoolExecutor createSafeThreadPool(String name, Integer threadCount, Integer queueSize, boolean isPreStart) {
		ThreadPoolExecutor tp = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(queueSize, true), new ThreadFactory() {
					private AtomicInteger counter = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r);
						t.setName(name + counter.getAndIncrement());
						return t;
					}
				});

		if (isPreStart)
			tp.prestartAllCoreThreads();

		return tp;
	}
}
