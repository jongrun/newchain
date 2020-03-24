package sysu.newchain.rpc.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;








import org.slf4j.Logger;
import org.slf4j.LoggerFactory;








import sysu.newchain.common.ThreadUtil;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.core.Transaction.Respone;
import sysu.newchain.common.format.Base58;
import sysu.newchain.rpc.api.ChainAPI;
import sysu.newchain.rpc.dto.BlockDTO;
import sysu.newchain.rpc.dto.TxRespDTO;
import sysu.newchain.tools.GenTransaction;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

public class JsonRpcClient {
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcClient.class);
	
	private static final String endpoint = "http://localhost:8080/newchain";
	private static AtomicLong total = new AtomicLong(0);
	private static AtomicLong count = new AtomicLong(0);
	private static int testNum = 50000;
	private static ExecutorService sendRequestExecutor = ThreadUtil.createExecutorService("sendRequestExecutor-thread", (int) (Runtime.getRuntime().availableProcessors() * 30), true);
//	private static CountDownLatch countDownLatch = new CountDownLatch(testNum);
	
	public static void main(String[] args) {
		try {
			URL url = new URL(endpoint);
			Map<String, String> map = new HashMap<String, String>();
			JsonRpcHttpClient client = new JsonRpcHttpClient(url, map);
			JsonRpcClient jsonRpcClient = new JsonRpcClient();
			ChainAPI chainAPI = jsonRpcClient.chainAPI(client);
			logger.info("start");
			long start = System.currentTimeMillis();
			for (int i = 0; i < testNum; i++) {
				final int index = i;
				CompletableFuture<TxRespDTO> future = CompletableFuture.supplyAsync(()->{
					TxRespDTO txRespDTO = null;
					try {
						Transaction tx = GenTransaction.genTransaction();
						txRespDTO = chainAPI.insertTransaction(
								tx.getFrom().getEncodedBase58(), 
								tx.getTo().getEncodedBase58(), 
								tx.getAmount(),
								tx.getTime(),
								Base58.encode(tx.getSign()), 
								Base58.encode(tx.getPubKey()),
								Base58.encode(tx.getData()));
					} catch (Exception e1) {
						logger.error("", e1);
					}
					return txRespDTO;
				}, sendRequestExecutor).whenComplete((v, e)->{
//					countDownLatch.countDown();
//					logger.info("count: {},%10={}, retCode: {}, msg: {}, hash: {}, height: {}", count.get(), count.get() % 10, v.getCode(), v.getMsg(), v.getTxHash(), v.getHeight());
					long c;
					if ((c = count.incrementAndGet()) % 1000 == 0) {
						float cost = (float) ((System.currentTimeMillis() - start) / 1000.0);
						logger.info("tx num: {}, cost time: {}, tps: {}", c, cost, c / cost);
					}
				}).exceptionally(e->{
					logger.error("", e);
					return null;
				});
			}
//			countDownLatch.await();
			sendRequestExecutor.shutdown();
			sendRequestExecutor.awaitTermination(1, TimeUnit.HOURS);
			long end = System.currentTimeMillis();
			float cost = (float) ((end - start) / 1000.0);
			logger.info("cost time: {}, average: {}, tps: {}", cost, cost / testNum, testNum / cost);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public ChainAPI chainAPI(JsonRpcHttpClient jsonRpcHttpClient){
		return ProxyUtil.createClientProxy(getClass().getClassLoader(), ChainAPI.class, jsonRpcHttpClient);
	}
}
