package sysu.newchain.client;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.ReplyMsg;
import sysu.newchain.core.Transaction;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.properties.NodesProperties;
import sysu.newchain.rpc.dto.TxRespDTO;

public class Client extends ReceiverAdapter{
	private static final Logger logger = LoggerFactory.getLogger(Client.class);
	private JChannel channel;
	private ECKey ecKey;
	private int f = (NodesProperties.getNodesSize() - 1) / 3;
	
	// 收集对交易请求的响应 <Replica, RetCode>
	RequestVoteTable<Long, Integer> requestVoteTable = new RequestVoteTable<Long, Integer>();
	
	// 请求超时处理
	private Map<CompletableFuture, Long> timeoutMap = new ConcurrentHashMap<CompletableFuture, Long>();
	private ScheduledExecutorService scheduler = null;
	
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
		scheduler = Executors.newSingleThreadScheduledExecutor();
//		scheduler.scheduleAtFixedRate(new Runnable() {
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				Iterator<Map.Entry<CompletableFuture, Long>> it = timeoutMap.entrySet().iterator();
//				   while(it.hasNext()){
//				       Map.Entry<CompletableFuture, Long> entry = it.next();
//				       if(System.currentTimeMillis() - entry.getValue() >= 2 * 1000) {
//				    	   entry.getKey().completeExceptionally(new Exception("Request time out"));
//				       }
//				   }
//			}
//		}, 1, 1, TimeUnit.SECONDS);
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
				if (!msgWithSign.verifySign(Base58.decode(NodesProperties.get(replyMsg.getReplica()).getPubKey()))) {
					logger.error("sign for replyMsg from node {} error", replyMsg.getReplica());
				}
				boolean ret = requestVoteTable.add(index, replyMsg.getReplica(), replyMsg.getRetCode(), f + 1);
				if (ret) {
					TxRespDTO dto = new TxRespDTO();
					dto.setBlockTime(replyMsg.getBlockTime());
					dto.setCode(replyMsg.getRetCode());
					dto.setHeight(replyMsg.getHeight());
					dto.setMsg(replyMsg.getRetMsg());
					dto.setTxHash(index);
					requestVoteTable.notifyAndRemove(index, dto);
				}
				break;

			default:
				break;
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public CompletableFuture<TxRespDTO> sendTxToServer(Transaction tx) throws Exception {
		String txHash = Hex.encode(tx.getHash());
		logger.debug("send tx to server, txHash: {}", txHash);
		CompletableFuture<TxRespDTO> txRespFuture = waitResp(txHash);
		// 客户端签名
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
	
	private CompletableFuture<TxRespDTO> waitResp(String txHash){
		CompletableFuture<TxRespDTO> txRespFuture = new CompletableFuture<TxRespDTO>();
		requestVoteTable.create(txHash, txRespFuture);
		// 超时处理
		CompletableFuture<Object> timeoutFuture = new CompletableFuture<Object>();
		timeoutMap.put(timeoutFuture, System.currentTimeMillis());
		timeoutFuture.exceptionally(e->{
			logger.error("timeoutFuture exception");
			timeoutMap.remove(timeoutFuture);
			txRespFuture.completeExceptionally(e);
			requestVoteTable.remove(txHash);
			return null;
		});
		txRespFuture.whenComplete((v, e)->{
			timeoutMap.remove(timeoutFuture);
		});
		return txRespFuture;
	}
	
}
