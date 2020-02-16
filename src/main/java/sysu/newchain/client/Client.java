package sysu.newchain.client;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.ReplyMsg;
import sysu.newchain.consensus.pbft.msg.TxMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;

public class Client extends ReceiverAdapter{
	Logger logger = LoggerFactory.getLogger(Client.class);
	private JChannel channel;
	private ECKey ecKey;
	
	public Client(String name){

	}
	
	public Client(ECKey ecKey, String name) throws Exception {
		channel = new JChannel();
		channel.setDiscardOwnMessages(true);
		channel.setName(name);
		channel.setReceiver(this);
		this.ecKey = ecKey;
	}
	
	public void setECKey(ECKey ecKey) {
		this.ecKey = ecKey;
	}
	
	public void start() throws Exception {
		channel.connect("responser");
	}
	
	public JChannel getChannel() {
		return channel;
	}
	
	@Override
	public void receive(Message msg) {
		try {
			MsgWithSign msgWithSign = new MsgWithSign(msg.getBuffer());
			switch (msgWithSign.getMsgCase()) {
			case REPLYMSG:
				ReplyMsg replyMsg = msgWithSign.getReplyMsg();
				logger.debug("receive reply from server node {}", replyMsg.getReplica());
				break;

			default:
				break;
			}
		} catch (InvalidProtocolBufferException e) {
			logger.error("", e);
		}
	}
	
	public void sendTxToServer(Transaction tx) throws Exception {
		MsgWithSign txMsgWithSign = new MsgWithSign(tx);
		txMsgWithSign.calculateAndSetSign(ecKey);
		channel.send(new Message(getAddress(AppConfig.getNodeId()), txMsgWithSign.toByteArray()));
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
