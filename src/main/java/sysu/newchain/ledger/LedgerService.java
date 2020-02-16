package sysu.newchain.ledger;

import java.util.concurrent.CompletableFuture;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.core.Block;
import sysu.newchain.core.Transaction;
import sysu.newchain.core.Transaction.Respone;
import sysu.newchain.dao.AccountDao;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.dao.TransactionDao;

public class LedgerService {
	
	private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);
	
	private BlockDao blockDao = BlockDao.getInstance();
	private TransactionDao txDao = TransactionDao.getInstance();
	private AccountDao accountDao = AccountDao.getInstance();
	
	private static final LedgerService instance = new LedgerService();
	
	public static LedgerService getInstance() {
		return instance;
	}
	
	private LedgerService() {}
	
	public void excuteBlock(Block block, CompletableFuture<Block> curBlockFuture) throws RocksDBException, Exception {
		for (Transaction tx : block.getTransactions()) {
			if (tx.isOk()) {
				if (!accountDao.transfer(tx.getFrom(), tx.getTo(), tx.getAmount())) {
					tx.setResponse(Respone.LEDGER_ERROR);
				}
			}
		}
		curBlockFuture.complete(block);
		saveBlock(block);
	}
	
	public void saveBlock(Block block) throws Exception {
		blockDao.insertBlock(block);
	}
}