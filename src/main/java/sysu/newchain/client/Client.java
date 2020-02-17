package sysu.newchain.client;

import java.util.concurrent.CompletableFuture;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.ReplyMsg;
import sysu.newchain.consensus.pbft.msg.TxMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.properties.NodesProperties;
import sysu.newchain.rpc.dto.InsertTransRespDTO;

public class Client extends ReceiverAdapter{
	Logger logger = LoggerFactory.getLogger(Client.class);
	private JChannel channel;
	private ECKey ecKey;
	int f = (NodesProperties.getNodesSize() - 1) / 3;
	
	// <Replica, RetCode>
	RequestVoteTable<Long, Integer> requestVoteTable = new RequestVoteTable<Long, Integer>();
	
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
				String index = Hex.encode(replyMsg.getTxHash());
				logger.debug("receive replyMsg: {}", replyMsg);
				boolean ret = requestVoteTable.add(index, replyMsg.getReplica(), replyMsg.getRetCode(), f + 1);
				if (ret) {
					InsertTransRespDTO dto = new InsertTransRespDTO();
					dto.setBlockTime(replyMsg.getBlockTime());
					dto.setCode(replyMsg.getRetCode());
					dto.setHeight(String.valueOf(replyMsg.getHeight()));
					dto.setMsg(replyMsg.getRetMsg());
					dto.setTxHash(index);
					requestVoteTable.notifyAndRemove(index, dto);
				}
				break;

			default:
				break;
			}
		} catch (InvalidProtocolBufferException e) {
			logger.error("", e);
		}
	}
	
	public CompletableFuture<InsertTransRespDTO> sendTxToServer(Transaction tx) throws Exception {
		String txHash = Hex.encode(tx.getHash());
		CompletableFuture<InsertTransRespDTO> txRespFuture = new CompletableFuture<InsertTransRespDTO>();
		requestVoteTable.create(txHash, txRespFuture);
		tx.setClientPubKey(ecKey.getPubKeyAsBytes());
		MsgWithSign txMsgWithSign = new MsgWithSign(tx);
		txMsgWithSign.calculateAndSetSign(ecKey);
		channel.send(new Message(getAddress(AppConfig.getNodeId()), txMsgWithSign.toByteArray()));
		return txRespFuture;
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
