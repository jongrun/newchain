package sysu.newchain.consensus.pbft.msg.log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicLongMap;

import sysu.newchain.common.LockFactory;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.pbft.msg.CommitMsg;
import sysu.newchain.consensus.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.pbft.msg.PrePrepareMsg;
import sysu.newchain.consensus.pbft.msg.PrepareMsg;
import sysu.newchain.consensus.pbft.msg.log.PhaseShiftHandler.Status;

public class MsgLog {
	private static final Logger logger = LoggerFactory.getLogger(MsgLog.class);
	
	long f; // 可容忍拜占庭错误节点数
	
	private PhaseShiftHandler handler;
	
	// key(view, seqNum, digest) -> Phase
	Map<String, Status> statusMap = Maps.newConcurrentMap();
	Map<String, ReentrantLock> statusLockMap = Maps.newConcurrentMap();
	
	// key(view, reqNum) -> pre-prepare
	private Map<String, MsgWithSign> prePrepareMsgs = Maps.newConcurrentMap();
	
	// key(view, seqNum, digest) -> (replica -> prepare)
	private Map<String, Map<Long, MsgWithSign>> prepareMsgs = Maps.newConcurrentMap();
	private AtomicLongMap<String> prepareNum = AtomicLongMap.create();
	
	// key(view, seqNum, digest) -> (replica -> prepare)
	private Map<String, Map<Long, MsgWithSign>> commitMsgs = Maps.newConcurrentMap();
	private AtomicLongMap<String> commitNum = AtomicLongMap.create();
	
	public MsgLog() {
		// TODO Auto-generated constructor stub
	}

	public long getF() {
		return f;
	}

	public void setF(long f) {
		this.f = f;
	}

	public PhaseShiftHandler getHandler() {
		return handler;
	}

	public void setHandler(PhaseShiftHandler handler) {
		this.handler = handler;
	}
	
	public Map<String, MsgWithSign> getPrePrepareMsgs() {
		return prePrepareMsgs;
	}

	public Map<String, Map<Long, MsgWithSign>> getPrepareMsgs() {
		return prepareMsgs;
	}

	public Map<String, Map<Long, MsgWithSign>> getCommitMsgs() {
		return commitMsgs;
	}

	public void add(MsgWithSign msgWithSign) throws Exception{
		switch (msgWithSign.getMsgCase()) {
			case PREPREPAREMSG:
				addPrePrepare(msgWithSign);
				break;
			case PREPAREMSG:
				addPrepare(msgWithSign);
				break;
			case COMMITMSG:
				addCommit(msgWithSign);
				break;
			default:
				break;
		}
	}
	
	private void addPrePrepare(MsgWithSign prePrepareMsgWithSign) {
		PrePrepareMsg prePrepareMsg = prePrepareMsgWithSign.getPrePrepareMsg();
		String prePreparekey = getKey(prePrepareMsg);
		prePrepareMsgs.put(prePreparekey, prePrepareMsgWithSign);
		String statusKey = getKey(prePrepareMsg.getView(), prePrepareMsg.getSeqNum(), prePrepareMsg.getDigestOfBlock());
		Lock lock = LockFactory.getLock(statusKey);
		if (!statusMap.containsKey(statusKey)) {
			try {
				lock.lock();
				statusMap.put(statusKey, Status.PRE_PREPARED);
				handler.enterPrepare(prePrepareMsg.getView(), prePrepareMsg.getSeqNum(), prePrepareMsg.getDigestOfBlock());
				checkIsPrepared(prePrepareMsg.getView(), prePrepareMsg.getSeqNum(), prePrepareMsg.getDigestOfBlock());
			} finally {
				lock.unlock();
			}
		}
	}
	
	private void addPrepare(MsgWithSign prepareMsgWithSign) throws Exception{
		PrepareMsg prepareMsg = prepareMsgWithSign.getPrepareMsg();
		String prepareKey = getKey(prepareMsg);
		
		if (prepareMsgs.containsKey(prepareKey)) {
			// 大部分情况下为此分支，不用加锁，提高效率
		}
		else {
			synchronized (prepareMsgs) {
				if (!prepareMsgs.containsKey(prepareKey)) {
					prepareMsgs.put(prepareKey, new HashMap<Long, MsgWithSign>((int) (2 * f)));
				}
			}
		}
		Map<Long, MsgWithSign> prepareMsgsForN = prepareMsgs.get(prepareKey);;
		long num = prepareNum.get(prepareKey);
		if (num >= 2 * f) {
			logger.debug("has accepted >=2f (={}) prepare msgs", 2 * f);
			return;
		}
		synchronized (prepareMsgsForN) {
			if (num < 2 * f) {
				// 检测是否已经接受该节点的prepare
				if (prepareMsgsForN.containsKey(prepareMsg.getReplica())) {
					logger.debug("has accepted the prepare msg, {}", prepareMsg);
					return;
				}
				logger.debug("add msg {}", prepareMsg);
				prepareMsgsForN.put(prepareMsg.getReplica(), prepareMsgWithSign);
				num = prepareNum.incrementAndGet(prepareKey);
				if (num >= 2 * f) {
					checkIsPrepared(prepareMsg.getView(), prepareMsg.getSeqNum(), prepareMsg.getDigestOfBlock());
				}
			}
			else {
				logger.debug("has accepted >=2f (={}) prepare msgs", 2 * f);
			}
		}
	}
	
	private void addCommit(MsgWithSign commitMsgWithSign) throws Exception{
		CommitMsg commitMsg = commitMsgWithSign.getCommitMsg();
		String commitKey = getKey(commitMsg);
		if (commitMsgs.containsKey(commitKey)) {
			
		}
		else {
			synchronized (commitMsgs) {
				if (!commitMsgs.containsKey(commitKey)) {
					commitMsgs.put(commitKey, new HashMap<Long, MsgWithSign>((int) (2 * f + 1)));
				}
			}
		}
		Map<Long, MsgWithSign> commitMsgsForN = commitMsgs.get(commitKey);
		long num = commitNum.get(commitKey);
		// 检测是否已经接收足够commit消息
		if (num >= 2 * f + 1) {
			logger.debug("has accepted >=2f + 1 (={}) commit msgs", 2 * f + 1);
			return;
		}
		synchronized (commitMsgsForN) {
			if (num < 2 * f + 1) {
				// 检测是否已经接受该节点的commit
				if (commitMsgsForN.containsKey(commitMsg.getReplica())) {
					logger.debug("has accepted the commit msg, {}", commitMsg);
					return;
				}
				logger.debug("add msg {}", commitMsg);
				commitMsgsForN.put(commitMsg.getReplica(), commitMsgWithSign);
				num = commitNum.incrementAndGet(commitKey);
				if (num >= 2 * f + 1) {
					checkIsCommitted(commitMsg.getView(), commitMsg.getSeqNum(), commitMsg.getDigestOfBlock());
				}
			}
			else {
				logger.debug("has accepted >=2f + 1 (={}) commit msgs", 2 * f + 1);
			}
		}
	}
	
	public void checkIsPrepared(long view, long seqNum, byte[] digest) {
		String key = getKey(view, seqNum, digest);
		Status status = statusMap.get(key);
		if (prepareNum.get(key) >= 2 * f && Status.PRE_PREPARED.equals(status)) {
			synchronized (status) {
				if (Status.PRE_PREPARED.equals(status)) {
					statusMap.put(key, Status.PREPARED);
					logger.debug("enters commit phase");
					checkIsCommitted(view, seqNum, digest);
					handler.enterCommit(view, seqNum, digest);
				}
			}
//			CommitMsg commitMsg = prepareMsg.createCommitMsg(nodeId);
//			MsgWithSign commitMsgWithSign = new MsgWithSign();
//			commitMsgWithSign.setCommitMsg(commitMsg);
//			commitMsgWithSign.calculateAndSetSign(ecKey);
//			addCommit(commitMsgWithSign);
//			channel.send(new Message(null, commitMsgWithSign.toByteArray()));
		}
	}
	
	public void checkIsCommitted(long view, long seqNum, byte[] digest) {
		if (isCommitted(view, seqNum, digest)) {
//			handler.enterCommit(view, seqNum, digest);
		}
	}
	
	private boolean isPrePrepared(long view, long seqNum, byte[] digest) {
		String key = getKey(view, seqNum, digest);
		return Status.PRE_PREPARED.equals(statusMap.get(key));
	}
	
	private boolean isPrepared(long view, long seqNum, byte[] digest) {
		String preKey = getKey(view, seqNum, digest);
		// if and only if replica has inserted in its log: 
		// a pre-prepare(with request block) in view v with sequence number n, 
		// and 2f prepares from different backups that match the pre-prepare. 
//		if (prepareNum.get(preKey) >= 2 * f) {
//			// verify whether the prepares match the pre-prepare by checking that they have the same view, sequence number, and digest. 
//			String prePrepareKey = String.format("view:%d,&seqNum:%d", view, seqNum);
//			if (prePrepareMsgs.containsKey(prePrepareKey)) {
//				PrePrepareMsg prePrepareMsg = prePrepareMsgs.get(prePrepareKey).getPrePrepareMsg();
//				if (Hex.encode(prePrepareMsg.getDigestOfBlock()).equals(Hex.encode(digest))) {
//					return true;
//				}
//			}
//		}
		if (prepareNum.get(preKey) >= 2 * f && isPrePrepared(view, seqNum, digest)) {
			return true;
		}
		return false;
	}
	
	private boolean isCommitted(long view, long seqNum, byte[] digest) {
		String commitKey = String.format("view:%d,&seqNum:%d,digest%s", view, seqNum, Hex.encode(digest));
		// if and only if prepared(view, seqNum, digest) is true and 
		// has accepted 2f + 1 commits (possibly including its own) from different replicas that match the pre-prepare; 
		// a commit matches a pre-prepare if they have the same view, sequence number, and digest. 
		if (commitNum.get(commitKey) >= 2 * f + 1 && isPrepared(view, seqNum, digest)) {
			return true;
		}
		return false;
	}
	
	private String getKey(PrePrepareMsg prePrepareMsg){
		return getKey(prePrepareMsg.getView(), prePrepareMsg.getSeqNum());
	}
	
	private String getKey(PrepareMsg prepareMsg){
		return getKey(prepareMsg.getView(), prepareMsg.getSeqNum(), prepareMsg.getDigestOfBlock());
	}
	
	private String getKey(CommitMsg commitMsg){
		return getKey(commitMsg.getView(), commitMsg.getSeqNum(), commitMsg.getDigestOfBlock());
	}
	
	private String getKey(long view, long seqNum){
		return String.format("view:%d,seqNum:%d", view, seqNum);
	}
	
	private String getKey(long view, long seqNum, byte[] digest){
		return String.format("view:%d,seqNum:%d,digest:%s", view, seqNum, Hex.encode(digest));
	}
	
}
