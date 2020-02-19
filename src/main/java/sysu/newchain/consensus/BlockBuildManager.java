package sysu.newchain.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.ThreadUtil;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.Pbft;
import sysu.newchain.consensus.pbft.Pbft.RoleChange;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;
import sysu.newchain.rpc.dto.TxRespDTO;

public class BlockBuildManager implements RoleChange{
	private static final Logger logger = LoggerFactory.getLogger(BlockBuildManager.class);
	private static BlockBuildManager blockBuildManager = new BlockBuildManager();
	public static BlockBuildManager getInstance(){
		return blockBuildManager;
	}
	private BlockBuildManager() {}
	
	private Pbft pbft;
	private RequestResponer requestResponer;
	
	// 判断是否开始组块，切换为leader时才组块
	private AtomicBoolean startBuildBlock = new AtomicBoolean(false);
	// 用于统计当前累计块大小，以决定是否出块
	private AtomicLong blockSize = new AtomicLong();
	
	// 待打包成块的新交易队列、待转发给leader的交易队列、待广播的块队列
	private BlockingQueue<Transaction> newTxQueue = new LinkedBlockingQueue<Transaction>(100000);
	private BlockingQueue<Transaction> resendQueue = new LinkedBlockingQueue<Transaction>(100000);
	private BlockingQueue<Block> newBlockQueue = new LinkedBlockingQueue<Block>(1000);
	
	// 块处理
	private ConcurrentHashMap<Long, CompletableFuture<Block>> blockFutures = new ConcurrentHashMap<>();
	
	// 执行操作的几个线程
	private ExecutorService blockBuildExecutor;
	private ExecutorService resendTxExecutor;
	private ExecutorService blockBroadcastExecutor;
	
	// 对应各个线程的任务
	Runnable blockBuildTask = new Runnable() {
		private long lastTime = 0;
		
		@Override
		public void run() {
			while (true) {
				if (!startBuildBlock.get()) {
					try {
						Thread.sleep(10);
						continue;
					} catch (InterruptedException e) {
						continue;
					}
				}
				
				// 交易数量达到，或 块大小达到，或 超过出块时间间隔
				if (newTxQueue.size() >= 128 
						|| blockSize.get() >= 8 * 1000 * 1000
						|| (System.currentTimeMillis() - lastTime) > 100 && newTxQueue.size() > 0) {
					Block block;
					try {
						block = prepareBlock();
						if (block != null) {
							try {
								logger.debug("offer block into newBlockQueue");
								newBlockQueue.offer(block, 5, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							lastTime = System.currentTimeMillis();
						}
					} catch (Exception e) {
						logger.error("", e);
					}
				}
				if (newTxQueue.size() < 128 && blockSize.get() < 8 * 1000 * 1000) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						continue;
					}
				}
			}
		}
	};
	
	Runnable resendTxTask = new Runnable() {
		@Override
		public void run() {
			while (true) {
				if (startBuildBlock.get()) {
					try {
						Thread.sleep(100);
						continue;
					} catch (InterruptedException e) {
						continue;
					}
				}
				Transaction tx = null;
				try {
					tx = resendQueue.take();
					if (tx == null) {
						continue;
					}
					requestResponer.sendTx(pbft.getPrimary(), tx);
				} catch (Exception e) {
					if (tx !=null) {
						try {
							Thread.sleep(100);
							resendQueue.offer(tx, 5, TimeUnit.SECONDS);
						} catch (InterruptedException e1) {
						}
					}
					logger.error("", e);
				}
				
			}
		}
	};
	
	Runnable blockBroadcastTask = new Runnable() {
		@Override
		public void run() {
			while (true) {
				Block block = null;
				try {
					block = newBlockQueue.take();
					broadcastBlock(block);
				} catch (InterruptedException e) {
					logger.error("", e);
				}
			}
		}
	};
	
	public void init() throws Exception {
		logger.info("init blockBuildManager");
		pbft = Pbft.getInstance();
		requestResponer = RequestResponer.getInstance();
		blockBuildExecutor = ThreadUtil.createExecutorService("blockBuildExecutor-thread", 1, true);
		resendTxExecutor = ThreadUtil.createExecutorService("resendTxExecutor-thread", 1, true);
		blockBroadcastExecutor = ThreadUtil.createExecutorService("blockBroadcastExecutor-thread", 1, true);
	}
	
	public void start(){
		blockBuildExecutor.submit(blockBuildTask);
		resendTxExecutor.submit(resendTxTask);
		blockBroadcastExecutor.submit(blockBroadcastTask);
	}
	
	public void broadcastBlock(Block block) {
		if (block == null) {
			return;
		}
		logger.debug("is startBuildBlock: {}", startBuildBlock.get());
		if (!startBuildBlock.get()) {
			unBuildBlock(block);
			return;
		}
		try {
			MsgWithSign msgWithSign = new MsgWithSign();
			msgWithSign.setBlock(block);
			logger.debug("set a block into pbft");
			pbft.onRequest(msgWithSign.toByteArray());
			
		} catch (Exception e) {
			unBuildBlock(block);
			logger.error("", e);
		}
	}
	
	/**
	 * 广播不成功，转给新的leader处理
	 * @param block
	 */
	protected void unBuildBlock(Block block) {
//		//不是本节点组的block,不处理
//		if(!rsm.getNodeId().equals(block.getHeader().getNodeSign()))
//			return;
		logger.debug("unBuildBlock");
		for(Transaction tx: block.getTransactions()) {
			try {
				resendQueue.offer(tx, 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error("unBuildBlock:{}",e);
			}
		}
	}
	
	public Block prepareBlock() throws Exception {
		List<Transaction> transactions = new ArrayList<Transaction>(128);
		newTxQueue.drainTo(transactions, 128);
		if (transactions == null || transactions.size() == 0) {
			return null;
		}
		logger.debug("build a block with {} transactions", transactions.size());
//		blockSize.set(0); // TODO 有问题，待调整
		for (Transaction tx : transactions) {
			blockSize.addAndGet(-tx.calculateSize());
		}
		
		logger.debug("rest block size: {}", blockSize.get());
		
		Block block = new Block();
		try {
			BlockHeader header = new BlockHeader();
//			header.setHeight(pbft.incrementAndGetSeqNum());
//			long preHeight = header.getHeight() - 1;
//			CompletableFuture<byte[]> preFuture = blockFutures.get(preHeight);
//			byte[] preHash = null;
//			if (preFuture != null) {
//				preHash = preFuture.get();
//			}
//			else {
//				preHash = blockDao.getBlock(preHeight).getHash();
//			}
//			header.setPrehash(preHash);
			header.setTime(System.currentTimeMillis() + "");
			block.setHeader(header);
			block.setTransactions(transactions);
			return block;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	public CompletableFuture<TxRespDTO> pushTransaction(Transaction transaction) throws Exception{
		logger.debug("push tx, size: {}", transaction.calculateSize());
		if (pbft.isPrimary()) {
			logger.debug("new tx");
			newTxQueue.offer(transaction, 5, TimeUnit.SECONDS);
			blockSize.addAndGet(transaction.calculateSize());
		}
		else {
			logger.info("resend tx: {}", Hex.encode(transaction.getHash()));
			resendQueue.offer(transaction, 5, TimeUnit.SECONDS);
		}
		return null;
	}
	
	private synchronized void startBuild(){
		logger.debug("start build block, resendQueue size: {}, newTxQueue size: {}", resendQueue.size(), newTxQueue.size());
		resendQueue.drainTo(newTxQueue);
		blockSize.set(0);
		for (Transaction tx : newTxQueue) {
			try {
				blockSize.addAndGet(tx.calculateSize());
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		logger.debug("blockSize: {}", blockSize.get());
		startBuildBlock.set(true);
	}
	
	private synchronized void stopBuild() {
		logger.debug("stop build block, resendQueue size: {}, newTxQueue size: {}", resendQueue.size(), newTxQueue.size());
		newTxQueue.drainTo(resendQueue);
		blockSize.set(0);
		startBuildBlock.set(false);
	}

	@Override
	public void roleChanged(boolean isPrimary) {
		logger.info("role change, isPrimary: {}, primary: {}", isPrimary, pbft.getPrimary());
		if (isPrimary) {
			startBuild();
		}
		else {
			stopBuild();
		}
	}
}
