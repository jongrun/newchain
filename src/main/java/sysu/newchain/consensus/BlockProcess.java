package sysu.newchain.consensus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.BlockMsg;
import sysu.newchain.consensus.pbft.msg.ReplyMsg;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.ledger.LedgerService;
import sysu.newchain.server.Server;

public class BlockProcess {
	private BlockDao blockDao = BlockDao.getInstance();
	private LedgerService ledgerService = LedgerService.getInstance();
	private ConcurrentHashMap<Long, CompletableFuture<Block>> blockFutures = new ConcurrentHashMap<>();
	private Server server = Server.getInstance();
	
	public void commited(long seqNum, long view, Block block) throws Exception {
		block.getHeader().setHeight(seqNum);
		CompletableFuture<Block> curBlockFuture = new CompletableFuture<Block>();
		blockFutures.put(seqNum, curBlockFuture);
		
		// 验证区块（区块间可并行）
		block.verifyTransactions();
		calculateBlockHash(block);
		// 执行区块（不可并行）
		ledgerService.excuteBlock(block, curBlockFuture);
		
		for (Transaction tx : block.getTransactions()) {
			ReplyMsg replyMsg = new ReplyMsg();
			replyMsg.setView(view);
//			replyMsg.setClient(Hex.encode(tx.getClientPubKey()));
			replyMsg.setRetCode(tx.getRetCode());
			replyMsg.setRetMsg(tx.getRetMsg());
			server.sendTxResp(Hex.encode(tx.getClientPubKey()), replyMsg);
		}
	}
	
	public void calculateBlockHash(Block block) throws Exception{
		BlockHeader header = block.getHeader();
		long preHeight = header.getHeight() - 1;
		CompletableFuture<Block> preFuture = blockFutures.get(preHeight);
		byte[] preHash = null;
		if (preFuture != null) {
			// 等待上一个块执行完成
			preHash = preFuture.get().getHash();
		}
		else {
			preHash = blockDao.getBlock(preHeight).getHash();
		}
		header.setPrehash(preHash);
		block.calculateAndSetHash();
		
		if (preFuture != null) {
			blockFutures.remove(preHeight);
		}
	}
}
