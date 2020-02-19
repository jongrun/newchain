package sysu.newchain.consensus.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.consensus.BlockBuildManager;
import sysu.newchain.consensus.BlockProcessManager;
import sysu.newchain.consensus.RequestResponer;
import sysu.newchain.consensus.pbft.Pbft;
import sysu.newchain.service.BaseService;

public class ConsensusService implements BaseService{
	public static final Logger logger = LoggerFactory.getLogger(ConsensusService.class);
	private static final ConsensusService consensusService = new ConsensusService();
	public static ConsensusService getInstance() {
		return consensusService;
	}
	private ConsensusService() {}
	
	private RequestResponer requestResponer;
	private Pbft pbft;
	private BlockBuildManager blockBuildManager;
	private BlockProcessManager blockProcessManager;
	
	@Override
	public void init() throws Exception {
		logger.info("init consensus service");
		requestResponer = RequestResponer.getInstance();
		pbft = Pbft.getInstance();
		blockBuildManager = BlockBuildManager.getInstance();
		blockProcessManager = BlockProcessManager.getInstance();
		
		requestResponer.init();
		pbft.init();
		blockBuildManager.init();
		blockProcessManager.init();
		pbft.addRoleChangeListeners(blockBuildManager);
		pbft.setHandler(blockProcessManager);
	}
	
	@Override
	public void start() throws Exception {
		logger.info("start consensus service");
		pbft.start();
		blockBuildManager.start();
		requestResponer.start();
	}
	
}