package sysu.newchain.ledger.service;

import java.util.concurrent.CompletableFuture;

import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.core.Block;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.core.Transaction.Respone;
import sysu.newchain.dao.AccountDao;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.dao.TransactionDao;
import sysu.newchain.dao.service.DaoService;
import sysu.newchain.service.BaseService;

public class LedgerService implements BaseService{
	
	private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);
	
	private DaoService daoService;
	
	private static final LedgerService instance = new LedgerService();
	
	public static LedgerService getInstance() {
		return instance;
	}
	
	private LedgerService() {}
	
	@Override
	public void init(){
		logger.info("init ledger service");
		daoService = DaoService.getInstance();
	}
	
	@Override
	public void start(){
		logger.info("start ledger service");
	}
	
	public void excuteBlock(Block block, CompletableFuture<Block> curBlockFuture) throws RocksDBException, Exception {
		for (Transaction tx : block.getTransactions()) {
			// 检查交易是否重复（在历史出现过）
			if (daoService.getTransactionDao().getTransaction(tx.getHash()) != null) {
				tx.setResponse(Respone.DUP_TX_ERROR);
			}
			if (tx.isOk()) {
				// 转账是否成功（检查余额）
				if (!daoService.getAccountDao().transfer(tx.getFrom(), tx.getTo(), tx.getAmount())) {
					tx.setResponse(Respone.LEDGER_ERROR);
				}
			}
		}
		curBlockFuture.complete(block);
		saveBlock(block);
	}
	
	public void saveBlock(Block block) throws Exception {
		daoService.getBlockDao().insertBlock(block);
	}
}