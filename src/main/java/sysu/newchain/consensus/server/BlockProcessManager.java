package sysu.newchain.consensus.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.core.Block;
import sysu.newchain.common.core.BlockHeader;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.server.pbft.PbftHandler;
import sysu.newchain.consensus.server.pbft.msg.BlockMsg;
import sysu.newchain.consensus.server.pbft.msg.ReplyMsg;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.dao.service.DaoService;
import sysu.newchain.ledger.service.LedgerService;

public class BlockProcessManager implements PbftHandler{
	static final Logger logger = LoggerFactory.getLogger(BlockProcessManager.class);
	private static final BlockProcessManager blockProcessManager = new BlockProcessManager();
	public static BlockProcessManager getInstance() {
		return blockProcessManager;
	}
	private BlockProcessManager(){}
	
	private DaoService daoService;
	private LedgerService ledgerService;
	private RequestResponer responer;
	
	private ConcurrentHashMap<Long, CompletableFuture<Block>> blockFutures = new ConcurrentHashMap<>();
	
	public void init() throws Exception{
		logger.info("init BlockProcessManager");
		daoService = DaoService.getInstance();
		ledgerService = LedgerService.getInstance();
		responer = RequestResponer.getInstance();
		CompletableFuture<Block> lastBlockFuture = new CompletableFuture<Block>();
		long height = daoService.getBlockDao().getLastHeight();
		lastBlockFuture.complete(daoService.getBlockDao().getBlock(height));
		blockFutures.put(height, lastBlockFuture);
	}
	
	@Override
	public void committed(long seqNum, long view, BlockMsg blockMsg)
			throws Exception {
		logger.debug("commited height: {}", seqNum);
		Block block = blockMsg.toBlock();
		block.getHeader().setHeight(seqNum);
		CompletableFuture<Block> curBlockFuture = new CompletableFuture<Block>();
		blockFutures.put(seqNum, curBlockFuture);
		
		// 验证区块交易（区块间可并行）
		block.verifyTransactions();
		calculateBlockHash(block);
		// 执行区块（不可并行）
		ledgerService.excuteBlock(block, curBlockFuture);
		
		for (Transaction tx : block.getTransactions()) {
			ReplyMsg replyMsg = new ReplyMsg();
			replyMsg.setView(view);
			replyMsg.setTxHash(tx.getHash());
			replyMsg.setRetCode(tx.getRetCode());
			replyMsg.setRetMsg(tx.getRetMsg());
			replyMsg.setHeight(block.getHeader().getHeight());
			replyMsg.setBlockTime(block.getHeader().getTime());
			logger.debug("pubKey: {}", Base58.encode(tx.getClientPubKey()));
			logger.debug("{}", replyMsg);
			responer.sendTxResp(Base58.encode(tx.getClientPubKey()), replyMsg);
		}
	}
	
	/** 计算区块hash，依赖于前一区块hash
	 * @param block
	 * @throws Exception
	 */
	private void calculateBlockHash(Block block) throws Exception{
		BlockHeader header = block.getHeader();
		long preHeight = header.getHeight() - 1;
		CompletableFuture<Block> preFuture = blockFutures.get(preHeight);
		byte[] preHash = null;
		while(preFuture == null){
			preFuture = blockFutures.get(preHeight);
		}
		if (preFuture != null) {
			// 等待上一个块执行完成
			preHash = preFuture.get().getHash();
		}
		else {
//			logger.info("preHeight={}", preHeight);
//			logger.info("daoService={},daoService.getBlockDao()={}", daoService,daoService.getBlockDao());
//			logger.info("daoService.getBlockDao().getBlock(preHeight)=", daoService.getBlockDao().getBlock(preHeight));
			preHash = daoService.getBlockDao().getBlock(preHeight).getHash();
		}
		header.setPrehash(preHash);
		block.calculateAndSetHash();
		
		if (preFuture != null) {
			blockFutures.remove(preHeight);
		}
	}
}
