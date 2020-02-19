package sysu.newchain.dao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.dao.AccountDao;
import sysu.newchain.dao.BlockDao;
import sysu.newchain.dao.TransactionDao;
import sysu.newchain.service.BaseService;

public class DaoService implements BaseService{
	private static final Logger logger = LoggerFactory.getLogger(DaoService.class);
	private static final DaoService instance = new DaoService();
	public static DaoService getInstance() {
		return instance;
	}
	private DaoService() {}
	
	AccountDao accountDao;
	BlockDao blockDao;
	TransactionDao transactionDao;
	
	@Override
	public void init(){
		logger.info("init dao service");
		accountDao = AccountDao.getInstance();
		blockDao = BlockDao.getInstance();
		transactionDao = TransactionDao.getInstance();
		accountDao.init();
		blockDao.init();
		transactionDao.init();
	}
	
	@Override
	public void start(){
		logger.info("start dao service");
	}
	
	public AccountDao getAccountDao() {
		return accountDao;
	}

	public BlockDao getBlockDao() {
		return blockDao;
	}

	public TransactionDao getTransactionDao() {
		return transactionDao;
	}
}
