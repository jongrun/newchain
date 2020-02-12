package sysu.newchain.consensus.pbft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.BlockMsg;
import sysu.newchain.consensus.pbft.msg.CommitMsg;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.PrePrepareMsg;
import sysu.newchain.consensus.pbft.msg.PrepareMsg;
import sysu.newchain.core.Block;
import sysu.newchain.properties.AppConfig;
import sysu.newchain.properties.NodeInfo;
import sysu.newchain.properties.NodesProperties;
import sysu.newchain.proto.BlockPb;
import sysu.newchain.proto.MsgWithSignPb;
import sysu.newchain.proto.NewchainProto;
import sysu.newchain.proto.MsgWithSignPb.MsgCase;
import sysu.newchain.proto.PrePreparePb;
import sysu.newchain.proto.PreparePb;
import sysu.newchain.proto.TransactionPb;

/**
 * @Description TODO
 * @author jongliao
 * @date 2020年1月20日 上午10:24:02
 */
public class Pbft extends ReceiverAdapter{
	
	public static final Logger logger = LoggerFactory.getLogger(Pbft.class);
	
	JChannel channel;
	ECKey ecKey;
	long nodeId; // 节点id
	long size; // 总节点数
	long f; // 可容忍拜占庭错误节点数
	boolean isPrimary = false; // 是否为主节点
	AtomicLong view = new AtomicLong(0); // 视图编号
	AtomicLong seqNum = new AtomicLong(0); // 请求序列号（区块高度）
	
	List<Function<View, Void>> roleChangeListeners = new ArrayList<Function<View,Void>>();
	
	public Pbft() throws Exception {
		ecKey = ECKey.fromPrivate(Base58.decode(AppConfig.getNodePriKey()));
		channel = new JChannel();
		// 设置节点id
		nodeId = AppConfig.getNodeId();
		channel.setName(String.valueOf(nodeId));
		channel.setDiscardOwnMessages(true);
		size = NodesProperties.getNodesSize();
		f = (size - 1) / 3; // TODO 待确认
		// 设置消息接受器
		channel.setReceiver(this);
	}
	
	public void start() throws Exception {
		logger.info("start pbft");
		// 连接到指定集群
		channel.connect("pbft");
//		IpAddress physicalAddress = (IpAddress) channel.down(new Event( 
//				Event.GET_PHYSICAL_ADDRESS, channel.getAddress())); 
//		String ip = physicalAddress.getIpAddress().getHostAddress(); 
//		int port = physicalAddress.getPort();
//		System.out.println(ip + ":" + port);
		// 检测主节点
		checkPrimary();
		logger.info("nodeId: {}, primary: {}, view: {}, nodeSize: {}", nodeId, getPrimary(), view.get(), size);
	}
	
	public Address getAddress(long nodeId) {
		for (Address address: channel.getView().getMembers()) {
			if (address.toString().equals(String.valueOf(nodeId))) {
				return address;
			}
		}
		return null;
	}
	
	public boolean isPrimary() {
		return isPrimary;
	}
	
	public long getPrimary() {
		return view.get() % size;
	}

	private void stop() {
		channel.close();
	}
	
	public long incrementAndGetSeqNum() {
		return seqNum.incrementAndGet();
	}
	
	/**
	 * @Description: 添加角色转变监听器
	 * @param listener
	 * @return 是否添加成功
	 */
	public boolean addRoleChangeListeners(Function<View, Void> listener) {
		return roleChangeListeners.add(listener);
	}
	
	// key(view, reqNum) -> pre-prepare
	Map<String, MsgWithSign> prePrepareMsgs = new HashMap<>();
	
	// key(view, seqNum, digest) -> (replica -> prepare)
	Map<String, Map<Long, MsgWithSign>> prepareMsgs = new HashMap<>();
	
	// key(view, seqNum, digest) -> (replica -> prepare)
	Map<String, Map<Long, MsgWithSign>> commitMsgs = new HashMap<>();
	
	@Override
	public void receive(Message msg) {
		try {
			MsgWithSign msgWithSign = new MsgWithSign(msg.getBuffer());
//			logger.debug("bytes to sign: {}, pubKey: {}, sign: {}", 
//					Hex.encode(msgWithSign.getBytesToSign()), 
//					NodesProperties.get(getPrimary()).getPubKey(),
//					Hex.encode(msgWithSign.getSign()));
			logger.debug("msg type: {}", msgWithSign.getMsgCase());
			switch (msgWithSign.getMsgCase()) {
				case PREPREPAREMSG:
					onPrePrepare(msgWithSign);
					break;
				case PREPAREMSG:
					onPrepare(msgWithSign);
					break;
				case COMMITMSG:
					onCommit(msgWithSign);
				default:
					break;
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	/** 主节点接收到请求消息，目前只有区块请求，区块内包含交易列表，每个交易请求由发起的客户端签名
	 * @param operator
	 * @throws Exception
	 */
	public void onRequest(byte[] operator) throws Exception {
		logger.debug("onRequest");
		if (isPrimary) {
			MsgWithSign msgWithSign = new MsgWithSign(operator);
			switch (msgWithSign.getMsgCase()) {
			case BLOCKMSG:
				onBlock(msgWithSign.getBlockMsg());
				break;
			default:
				break;
			}
		}
		else {
			throw new Exception("not primary!");
		}
	}
	
	/** 接收到区块请求，进入pre-prepare阶段
	 * @param blockMsg
	 * @throws Exception
	 */
	private void onBlock(BlockMsg blockMsg) throws Exception {
		logger.debug("onBlock, enters pre-prepare phase");
		PrePrepareMsg prePrepareMsg = new PrePrepareMsg();
		prePrepareMsg.setView(view.get());
		// assigns a sequence number
		prePrepareMsg.setSeqNum(seqNum.incrementAndGet());
		prePrepareMsg.setBlockMsg(blockMsg);
		prePrepareMsg.calculateAndSetDigestOfBlock();
		MsgWithSign msgWithSign = new MsgWithSign();
		msgWithSign.setPrePrepareMsg(prePrepareMsg);
		msgWithSign.calculateAndSetSign(ecKey);
//		logger.debug("BytesToSign: {}, priKey {} , sign: {}", 
//				Hex.encode(msgWithSign.getBytesToSign()), 
//				ecKey.getPriKeyAsBase58(),
//				Hex.encode(msgWithSign.getSign()));
		logger.debug("appends the pre-prepare message to log");
		prePrepareMsgs.put(getKey(prePrepareMsg), msgWithSign);
		logger.debug("multicasts a pre-prepare message, {}", prePrepareMsg);
		channel.send(new Message(null, msgWithSign.toByteArray()));
	}
	
	/** 接收到pre-prepare消息
	 * @param msgWithSign
	 * @throws Exception
	 */
	private void onPrePrepare(MsgWithSign msgWithSign) throws Exception {
		logger.debug("onPrePrepare");
		PrePrepareMsg prePrepareMsg = msgWithSign.getPrePrepareMsg();
		// A backup accepts a pre-prepare message <PRE-PREPARE, v, n, d, block> provided: 
		// 1. the signatures in the request(transactions in block) 
		// 		and the pre-prepare message are correct 
		// 		and d is the digest for block message
		// 2. it is in view v
		// 3. (TODO) it has not accepted a pre-prepare message for view v and sequence number n containing a different digest
		// 4. (TODO) the sequence number in the pre-prepare message is between a low water mark, h, and a high water mark, H
		
		// 1
		// 验证主节点签名
		logger.debug("receive pre-prepare msg: {}", prePrepareMsg);
		if (!msgWithSign.verifySign(Base58.decode(NodesProperties.get(getPrimary()).getPubKey()))) {
			logger.error("message sign error, type: {}", msgWithSign.getMsgCase());
			return;
		}
		// 验证区块中每个交易请求的客户端签名
		BlockMsg blockmsg = prePrepareMsg.getBlockMsg();
		if (!blockmsg.verifyTxSigns()) {
			logger.error("pre-prepare: sign of tx message from client in block error, seqNum: {}", prePrepareMsg.getSeqNum());
			return;
		}
		
		// 2
		if (prePrepareMsg.getView() != this.view.get()) {
			logger.error("view in pre-prepare message does not match the cur view");
			return;
		}
		
		// 3
		String key = getKey(prePrepareMsg);
		if (prePrepareMsgs.containsKey(key) && !prePrepareMsgs.get(key).getPrePrepareMsg().getDigestOfBlock().equals(prePrepareMsg.getDigestOfBlock())) {
			logger.debug("has accepted a pre-prepare message for view v and sequence number n containing a different digest");
			return;
		}
		// TODO 待确认：prePrepareMsgs已有相同prePre，是否还接受
		
		// 4 TODO
		
		logger.debug("accepts the pre-prepare, enters prepare phase");
		PrepareMsg prepareMsg = prePrepareMsg.createPrepareMsg(this.nodeId);
		MsgWithSign prepareMsgWithSign = new MsgWithSign();
		prepareMsgWithSign.setPrepareMsg(prepareMsg);
		prepareMsgWithSign.calculateAndSetSign(ecKey);
		
		logger.debug("multicasting a PREPARE message, {}", prepareMsg);
		channel.send(new Message(null, prepareMsgWithSign.toByteArray()));
		
		logger.debug("adds both pre-prepare {} and prepare messages {} to its log", prePrepareMsg, prepareMsg);
		prePrepareMsgs.put(key, msgWithSign);
		addPrepare(prepareMsgWithSign);
	}

	/** 接收到prepare消息
	 * @param prepareMsgWithSign
	 * @throws Exception
	 */
	private void onPrepare(MsgWithSign prepareMsgWithSign) throws Exception{
		PrepareMsg prepareMsg = prepareMsgWithSign.getPrepareMsg();
		logger.debug("receive prepare msg: {}", prepareMsg);
		// A replica (including the primary) accepts prepare messages and adds them to its log provided 
		// 1. their signatures are correct, 
		// 2. their view number equals the replica’s current view, 
		// 3. and their sequence number is between h and H. (TODO) 
		// 1
		if (!prepareMsgWithSign.verifySign(Base58.decode(NodesProperties.get(prepareMsg.getReplica()).getPubKey()))) {
			logger.error("message sign error, type: {}", prepareMsgWithSign.getMsgCase());
			return;
		}
		// 2
		if (prepareMsg.getView() != view.get()) {
			return;
		}
		// 3 TODO

		addPrepare(prepareMsgWithSign);
	}
	
	/** 接收到commit消息
	 * @param commitMsgWithSign
	 * @throws Exception 
	 */
	private void onCommit(MsgWithSign commitMsgWithSign) throws Exception {
		CommitMsg commitMsg = commitMsgWithSign.getCommitMsg();
		logger.debug("receive commit msg: {}", commitMsg);
		// Replicas accept commit messages and insert them in their log provided 
		// they are properly signed, 
		// the view number in the message is equal to the replica’s current view, 
		// and the sequence number is between h and H
		// 1
		if (!commitMsgWithSign.verifySign(Base58.decode(NodesProperties.get(commitMsg.getReplica()).getPubKey()))) {
			logger.error("message sign error, type: {}", commitMsgWithSign.getMsgCase());
			return;
		}
		// 2
		if (commitMsg.getView() != view.get()) {
			return;
		}
		// 3 TODO
		addCommit(commitMsgWithSign);
	}
	
	public void addPrepare(MsgWithSign prepareMsgWithSign) throws Exception{
		PrepareMsg prepareMsg = prepareMsgWithSign.getPrepareMsg();
		String preKey = getKey(prepareMsg);
		
		if (!prepareMsgs.containsKey(preKey)) {
			prepareMsgs.put(preKey, new HashMap<Long, MsgWithSign>((int) (2 * f + 1)));
		}
		Map<Long, MsgWithSign> prepareMsgsForN = prepareMsgs.get(preKey);
		if (isPrepared(prepareMsg.getView(), prepareMsg.getSeqNum(), prepareMsg.getDigestOfBlock())) {
			logger.debug("the request block is prepared");
			return;
		}
		// 检测是否已经接受该节点的prepare
		if (prepareMsgsForN.containsKey(prepareMsg.getReplica())) {
			logger.debug("has accepted the prepare msg, {}", prepareMsg);
			return;
		}
		logger.debug("add msg {}", prepareMsg);
		prepareMsgsForN.put(prepareMsg.getReplica(), prepareMsgWithSign);
		if (isPrepared(prepareMsg.getView(), prepareMsg.getSeqNum(), prepareMsg.getDigestOfBlock())) {
			logger.debug("enters commit phase");
			CommitMsg commitMsg = prepareMsg.createCommitMsg(nodeId);
			MsgWithSign commitMsgWithSign = new MsgWithSign();
			commitMsgWithSign.setCommitMsg(commitMsg);
			commitMsgWithSign.calculateAndSetSign(ecKey);
			addCommit(commitMsgWithSign);
			channel.send(new Message(null, commitMsgWithSign.toByteArray()));
		}
	}
	
	public void addCommit(MsgWithSign commitMsgWithSign) throws Exception{
		CommitMsg commitMsg = commitMsgWithSign.getCommitMsg();
		String commitKey = getKey(commitMsg);
		
		if (!commitMsgs.containsKey(commitKey)) {
			commitMsgs.put(commitKey, new HashMap<Long, MsgWithSign>((int) (2 * f + 1)));
		}
		Map<Long, MsgWithSign> commitMsgsForN = commitMsgs.get(commitKey);
		// 检测是否已经接收足够commit消息
		if (commitMsgsForN.size() >= 2 * f + 1) {
			logger.debug("has accepted 2f + 1 (={}) prepare msgs", 2 * f + 1);
			return;
		}
		// 检测是否已经接受该节点的commit
		if (commitMsgsForN.containsKey(commitMsg.getReplica())) {
			logger.debug("has accepted the commit msg, {}", commitMsg);
			return;
		}
		logger.debug("add msg {}", commitMsg);
		commitMsgsForN.put(commitMsg.getReplica(), commitMsgWithSign);
		if (isCommitted(commitMsg.getView(), commitMsg.getSeqNum(), commitMsg.getDigestOfBlock())) {
			commit(commitMsg.getSeqNum());
		}
	}
	
	private boolean isPrepared(long view, long seqNum, byte[] digest) {
		String preKey = String.format("view:%d,&seqNum:%d,digest%s", view, seqNum, Hex.encode(digest));
		Map<Long, MsgWithSign> prepareMsgsForN = prepareMsgs.get(preKey);
		// if and only if replica has inserted in its log: 
		// a pre-prepare(with request block) in view v with sequence number n, 
		// and 2f prepares from different backups that match the pre-prepare. 
		if (prepareMsgsForN.size() >= 2 * f) {
			// verify whether the prepares match the pre-prepare by checking that they have the same view, sequence number, and digest. 
			String prePrepareKey = String.format("view:%d,&seqNum:%d", view, seqNum);
			if (prePrepareMsgs.containsKey(prePrepareKey)) {
				PrePrepareMsg prePrepareMsg = prePrepareMsgs.get(prePrepareKey).getPrePrepareMsg();
				if (Hex.encode(prePrepareMsg.getDigestOfBlock()).equals(Hex.encode(digest))) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isCommitted(long view, long seqNum, byte[] digest) {
		String commitKey = String.format("view:%d,&seqNum:%d,digest%s", view, seqNum, Hex.encode(digest));
		// if and only if prepared(view, seqNum, digest) is true and 
		// has accepted 2f + 1 commits (possibly including its own) from different replicas that match the pre-prepare; 
		// a commit matches a pre-prepare if they have the same view, sequence number, and digest. 
		Map<Long, MsgWithSign> commitMsgForN = commitMsgs.get(commitKey);
		if (commitMsgForN.size() >= 2 * f + 1 && isPrepared(view, seqNum, digest)) {
			return true;
		}
		return false;
	}
	
	private void commit(long seqNum) {
		logger.debug("commit seqNum: {}", seqNum);
	}
	
	private String getKey(PrePrepareMsg prePrepareMsg){
		return String.format("view:%d,&seqNum:%d", prePrepareMsg.getView(), prePrepareMsg.getSeqNum());
	}
	
	private String getKey(PrepareMsg prepareMsg){
		return String.format("view:%d,&seqNum:%d,digest%s", prepareMsg.getView(), prepareMsg.getSeqNum(), Hex.encode(prepareMsg.getDigestOfBlock()));
	}
	
	private String getKey(CommitMsg commitMsg){
		return String.format("view:%d,&seqNum:%d,digest%s", commitMsg.getView(), commitMsg.getSeqNum(), Hex.encode(commitMsg.getDigestOfBlock()));
	}
	
	@Override
	public void viewAccepted(View view) {
//		viewChange();
//		System.out.println("isPrimary:" + isPrimary());
	}
	
	private void viewChange() {
		view.incrementAndGet();
		checkPrimary();
	}
	
	private void checkPrimary() {
		boolean isPrimary = nodeId == getPrimary(); 
		if (isPrimary != this.isPrimary) {
			this.isPrimary = isPrimary;
			for (Function<View, Void> listener : roleChangeListeners) {
				listener.apply(channel.getView());
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		new Pbft().start();
//		System.out.println("测试能不能到这里");
	}
}