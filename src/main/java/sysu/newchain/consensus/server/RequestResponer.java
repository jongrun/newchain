package sysu.newchain.consensus.server;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.consensus.server.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.server.pbft.msg.ReplyMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;

public class RequestResponer{
	public static final Logger logger = LoggerFactory.getLogger(RequestResponer.class);
	
	private static RequestResponer responer = new RequestResponer();
	public static RequestResponer getInstance(){
		return responer;
	}
	private RequestResponer(){
		try {
			channel = new JChannel();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	private JChannel channel;
	private SchnorrKey ecKey;
	private RequestReceiver receiver;
	
	public void init(){
		logger.info("init RequestResponer");
		try {
			channel.setDiscardOwnMessages(true);
			channel.setName(String.valueOf(AppConfig.getNodeId()));
			receiver = new RequestReceiver();
			receiver.init();
			channel.setReceiver(receiver);
			ecKey = SchnorrKey.fromPrivate(Base58.decode(AppConfig.getNodePriKey()));
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public void start() throws Exception {
		logger.info("start responer, connect to responer cluster");
		channel.connect("responser");
	}
	
	public void sendTx(long nodeId, Transaction tx) throws Exception{
		logger.debug("server node {} send tx to node {}", channel.getName(), nodeId);
		MsgWithSign msgWithSign = new MsgWithSign(tx);
		channel.send(getAddress(String.valueOf(nodeId)), msgWithSign.toByteArray());;
	}
	
	public void sendTxResp(String client, ReplyMsg replyMsg) throws Exception {
		logger.debug("server node {} response to client {}", channel.getName(), client);
		MsgWithSign msgWithSign = new MsgWithSign();
		msgWithSign.setReplyMsg(replyMsg);
		msgWithSign.setId(channel.getName());
		msgWithSign.calculateAndSetSign(ecKey);
		channel.send(new Message(getAddress(client), msgWithSign.toByteArray()));
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
