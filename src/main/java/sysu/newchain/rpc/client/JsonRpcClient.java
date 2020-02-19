package sysu.newchain.rpc.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import sysu.newchain.common.format.Base58;
import sysu.newchain.core.Transaction;
import sysu.newchain.rpc.api.ChainAPI;
import sysu.newchain.rpc.dto.BlockDTO;
import sysu.newchain.rpc.dto.TxRespDTO;
import sysu.newchain.tools.GenTransaction;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

public class JsonRpcClient {
	private static final Logger logger = LoggerFactory.getLogger(JsonRpcClient.class);
	private static final String endpoint = "http://localhost:8080/newchain";
	static AtomicLong total = new AtomicLong(0);
	static AtomicLong count = new AtomicLong(0);
	static int num = 2;
	private static CountDownLatch countDownLatch = new CountDownLatch(num);
	public static void main(String[] args) {
		try {
			URL url = new URL(endpoint);
			Map<String, String> map = new HashMap<String, String>();
			JsonRpcHttpClient client = new JsonRpcHttpClient(url, map);
			JsonRpcClient jsonRpcClient = new JsonRpcClient();
			ChainAPI chainAPI = jsonRpcClient.chainAPI(client);
			long start = System.currentTimeMillis();
			for (int i = 0; i < num; i++) {
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
						e1.printStackTrace();
					}
					return txRespDTO;
				});
				
				future.whenComplete((v, e)->{
					countDownLatch.countDown();
					if (v.getCode() == 0) {
						logger.debug("ok, height: {}", v.getHeight());
					}
					else {
						logger.debug("retCode: {}, height: {}", v.getCode(), v.getHeight());
					}
//					total.addAndGet(System.currentTimeMillis() - cur);
//					if (blockDTO != null) {
//						logger.debug("height: {}", blockDTO.getHeader().getHeight());
//						logger.debug("hash: {}", blockDTO.getHeader().getHash());
//						logger.debug("tx size: {}", blockDTO.getTransactions().size());
//					}
//					else {
//						logger.debug("null");
//					}
				});
				Thread.sleep(100);
			}
//			for (int i = 0; i < num; i++) {
//				final long cur = System.currentTimeMillis();
//				final int index = i;
//				CompletableFuture<BlockDTO> future = CompletableFuture.supplyAsync(()->{
//					BlockDTO blockDTO = null;
//					try {
//						blockDTO = chainAPI.getBlock(index);
//					} catch (Exception e1) {
//						e1.printStackTrace();
//					}
//					return blockDTO;
//				});
//				
//				future.whenComplete((blockDTO, e)->{
//					countDownLatch.countDown();
////					total.addAndGet(System.currentTimeMillis() - cur);
////					if (blockDTO != null) {
////						logger.debug("height: {}", blockDTO.getHeader().getHeight());
////						logger.debug("hash: {}", blockDTO.getHeader().getHash());
////						logger.debug("tx size: {}", blockDTO.getTransactions().size());
////					}
////					else {
////						logger.debug("null");
////					}
//				});
//			}
			countDownLatch.await();
			long end = System.currentTimeMillis();
			float cost = (float) ((end - start) / 1000.0);
			logger.debug("cost time: {}, average: {}, tps: {}", cost, cost / num, num / cost);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public ChainAPI chainAPI(JsonRpcHttpClient jsonRpcHttpClient){
		return ProxyUtil.createClientProxy(getClass().getClassLoader(), ChainAPI.class, jsonRpcHttpClient);
	}
}
