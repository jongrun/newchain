package sysu.newchain.consensus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import sysu.newchain.consensus.pbft.msg.BlockMsg;
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
	
	public void commited(long seqNum, Block block) throws Exception {
		blockFutures.put(seqNum, new CompletableFuture<Block>());
		
		BlockHeader header = new BlockHeader();
		header.setHeight(seqNum);
		long preHeight = header.getHeight() - 1;
		CompletableFuture<Block> preFuture = blockFutures.get(preHeight);
		byte[] preHash = null;
		if (preFuture != null) {
			preHash = preFuture.get().getHash();
		}
		else {
			preHash = blockDao.getBlock(preHeight).getHash();
		}
		header.setPrehash(preHash);
		block.setHeader(header);
		block.calculateAndSetHash();
		
		block.verifyTransactions();
		ledgerService.excuteBlock(block);
		
		for (Transaction tx : block.getTransactions()) {
			// TODO server.responseTx
		}
	}
}
