package sysu.newchain.server;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.ConsensusService;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.ReplyMsg;
import sysu.newchain.consensus.pbft.msg.TxMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;

public class Server extends ReceiverAdapter{
	public static final Logger logger = LoggerFactory.getLogger(Server.class);
	
	private ConsensusService consensusService = ConsensusService.getInstance();
	private JChannel channel;
	ECKey ecKey;
	
	private static final Server SERVER = new Server(String.valueOf(AppConfig.getNodeId()));
	
	public static Server getInstance(){
		return SERVER;
	}
	
	private Server(String name){
		try {
			channel = new JChannel();
			channel.setDiscardOwnMessages(true);
			channel.setName(name);
			channel.setReceiver(this);
			ecKey = ECKey.fromPrivate(Base58.decode(AppConfig.getNodePriKey()));
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
		logger.debug("server get msg {}", msg);
		try {
			MsgWithSign msgWithSign = new MsgWithSign(msg.getBuffer());
			logger.debug("msg type: {}", msgWithSign.getMsgCase());
			switch (msgWithSign.getMsgCase()) {
			case TXMSG:
				logger.debug("get tx msg");
				// 验证客户端签名
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
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public void sendTx(long nodeId, Transaction tx) throws Exception{
		logger.debug("server node {} send tx to node {}", channel.getName(), nodeId);
		MsgWithSign msgWithSign = new MsgWithSign(tx);
		channel.send(getAddress(String.valueOf(nodeId)), msgWithSign.toByteArray());;
	}
	
	public void sendTxResp(String client, ReplyMsg replyMsg) throws Exception {
		logger.debug("server node {} response to client {}", client, channel.getName());
		replyMsg.setReplica(Long.valueOf(channel.getName()));
		MsgWithSign msgWithSign = new MsgWithSign();
		msgWithSign.setReplyMsg(replyMsg);
		msgWithSign.calculateAndSetSign(ecKey);
		channel.send(getAddress(client), msgWithSign.toByteArray());
	}
	
	public Address getAddress(String name) {
		for (Address address: channel.getView().getMembers()) {
			if (address.toString().equals(name)) {
				return address;
			}
		}
		return null;
	}
}
