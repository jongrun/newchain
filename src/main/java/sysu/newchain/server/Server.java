package sysu.newchain.server;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.ConsensusService;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.TxMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;

public class Server extends ReceiverAdapter{
	public static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	private ConsensusService consensusService = ConsensusService.getInstance();
	private JChannel channel;
	
	private static final Server responser = new Server(String.valueOf(AppConfig.getNodeId()));
	
	public static Server getInstance(){
		return responser;
	}
	
	private Server(String name){
		try {
			channel = new JChannel();
			channel.setDiscardOwnMessages(true);
			channel.setName(name);
			channel.setReceiver(this);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public void start() throws Exception {
		logger.info("start server, connect to responer cluster");
		channel.connect("responser");
		consensusService.start();
	}
	
	@Override
	public void receive(Message msg) {
		logger.debug("responer get msg {}", msg);
		try {
			MsgWithSign msgWithSign = new MsgWithSign(msg.getBuffer());
			byte[] sign = msgWithSign.getSign();
			logger.debug("msg type: {}", msgWithSign.getMsgCase());
			switch (msgWithSign.getMsgCase()) {
			case TXMSG:
				logger.debug("get tx msg");
				// ÑéÖ¤Ç©Ãû
				TxMsg txMsg = msgWithSign.getTxMsg();
				Transaction tx = msgWithSign.toTransaction();
				if (!msgWithSign.verifySign(tx.getClientPubKey())) {
					logger.error("verify sign of tx msg from client failed, txhash: {}, client pubKey: {}", Hex.encode(tx.getHash()), Hex.encode(tx.getClientPubKey()));
					break;
				}
				consensusService.pushTransaction(tx);
				break;
			default:
				break;
			};
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void sendTx(long nodeId, Transaction tx) throws Exception{
		logger.debug("responser node {} send tx to node {}", channel.getName(), nodeId);
		MsgWithSign msgWithSign = new MsgWithSign(tx);
		channel.send(getAddress(nodeId), msgWithSign.toByteArray());;
	}
	
	public Address getAddress(long nodeId) {
		for (Address address: channel.getView().getMembers()) {
			if (address.toString().equals(String.valueOf(nodeId))) {
				return address;
			}
		}
		return null;
	}
}
