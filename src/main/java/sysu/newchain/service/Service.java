package sysu.newchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.consensus.service.ConsensusService;
import sysu.newchain.dao.service.DaoService;
import sysu.newchain.ledger.service.LedgerService;

public class Service implements BaseService{
	private static final Logger logger = LoggerFactory.getLogger(DaoService.class);
	private static final Service instance = new Service();
	public static Service getInstance() {
		return instance;
	}
	private Service() {}
	
	
	ConsensusService consensusService;
	LedgerService ledgerService;
	DaoService daoService;
	
	@Override
	public void init() throws Exception{
		logger.info("init service");
		consensusService = ConsensusService.getInstance();
		ledgerService = LedgerService.getInstance();
		daoService = DaoService.getInstance();
		daoService.init(); 
		ledgerService.init();
		consensusService.init();
	}
	
	@Override
	public void start() throws Exception{
		logger.info("start service");
		daoService.start();
		ledgerService.start();
		consensusService.start();
	}
}
