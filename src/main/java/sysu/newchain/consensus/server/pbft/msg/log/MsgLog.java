package sysu.newchain.consensus.server.pbft.msg.log;

import java.nio.charset.Charset;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.protobuf.InvalidProtocolBufferException;

import sysu.newchain.common.ConcurrentKV;
import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.server.pbft.msg.BlockMsg;
import sysu.newchain.consensus.server.pbft.msg.CommitCertificate;
import sysu.newchain.consensus.server.pbft.msg.CommitMsg;
import sysu.newchain.consensus.server.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.server.pbft.msg.PrePrepareMsg;
import sysu.newchain.consensus.server.pbft.msg.PrepareCertificate;
import sysu.newchain.consensus.server.pbft.msg.PrepareMsg;
import sysu.newchain.consensus.server.pbft.msg.log.PhaseShiftHandler.Status;
import sysu.newchain.properties.AppConfig;

public class MsgLog {
	private static final Logger logger = LoggerFactory.getLogger(MsgLog.class);
	
	long f; // 可容忍拜占庭错误节点数
	
	private PhaseShiftHandler handler;
	
	static final String SEQ_NUM = "n";
	static final String VIEW = "v";
	static final String DIGEST = "d";
	static final String REPLICA = "i";
	
	// String(SEQ_NUM=n:VIEW=v:DIGEST=d) -> Status
	private ConcurrentKV statusMap = new ConcurrentKV("pbft/status.db");
	
	// String(SEQ_NUM=n:VIEW=v) -> pre-prepare
//	private ConcurrentKV prePrepareMsgs = new ConcurrentKV("pbft/prePrepareMsgs.db");
	private Map<String, MsgWithSign> prePrepareMsgs = Maps.newConcurrentMap();
	
	// String(SEQ_NUM=n:VIEW=v:DIGEST) -> (REPLICA -> prepare)
//	private ConcurrentKV prepareMsgs = new ConcurrentKV("pbft/prepareMsgs.db");
	private Map<String, Map<Long, MsgWithSign>> prepareMsgs = Maps.newConcurrentMap();
	// String(SEQ_NUM=n:VIEW=v:DIGEST=d) -> long
//	private ConcurrentKV prepareNum = new ConcurrentKV("pbft/prepareNum.db");
//	private AtomicLongMap<String> prepareNum = AtomicLongMap.create();
	
	// key(view, seqNum, digest) -> (replica -> commit)
	private Map<String, Map<Long, MsgWithSign>> commitMsgs = Maps.newConcurrentMap();
//	private AtomicLongMap<String> commitNum = AtomicLongMap.create();
//	// String(SEQ_NUM=n:VIEW=v:DIGEST=d:REPLICA=i) -> prepare
//	private ConcurrentKV commitMsgs = new ConcurrentKV("pbft/commitMsgs.db");
//	// String(SEQ_NUM=n:VIEW=v:DIGEST=d) -> long
//	private ConcurrentKV commitNum = new ConcurrentKV("pbft/commitNum.db");
	
	// key(view, seqNum, digest) -> Cert
	private Map<String, MsgWithSign> prepareCerts = Maps.newConcurrentMap();
	private Map<String, MsgWithSign> commitCerts = Maps.newConcurrentMap();
	
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
			case PREPARECERTIFICATE:
				addPrepareCert(msgWithSign);
				break;
			case COMMITCERTIFICATE:
				addCommitCert(msgWithSign);
				break;
			default:
				break;
		}
	}
	
	private void addPrePrepare(MsgWithSign prePrepareMsgWithSign) throws Exception {
		PrePrepareMsg prePrepareMsg = prePrepareMsgWithSign.getPrePrepareMsg();
		String prePreparekey = getKey(prePrepareMsg.getSeqNum(), prePrepareMsg.getView());
		// TODO 待确认：prePrepareMsgs已有相同prePre，是否还接受？暂定：不接受。问题：此时可能无法触发发送prepare
		MsgWithSign value = prePrepareMsgs.putIfAbsent(prePreparekey, prePrepareMsgWithSign);
		if (value != null) {
			logger.debug("has accepted a prePrepare with same seqNum {} and view {}", prePrepareMsg.getSeqNum(), prePrepareMsg.getView());
		}
		else {
			logger.debug("add msg {}", prePrepareMsg);
			String statusKey = getKey(prePrepareMsg.getSeqNum(), prePrepareMsg.getView(), prePrepareMsg.getDigestOfBlock());
			// 若状态为空，更新状态为PRE_PREPARED并进入准备阶段；否则，什么也不做
			String oldValue = statusMap.putIfAbsent(statusKey, Status.PRE_PREPARED.toString());
			if (oldValue == null) {
				logger.debug("enter prepare phase");
				// 检查是否已准备好，即是否已经有2f个匹配的prepare消息
				checkIsPrepared(prePrepareMsg.getSeqNum(), prePrepareMsg.getView(), prePrepareMsg.getDigestOfBlock());
			}
		}
	}
	
	private void addPrepare(MsgWithSign prepareMsgWithSign) throws Exception{
		PrepareMsg prepareMsg = prepareMsgWithSign.getPrepareMsg();
//		String prepareKey = getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock(), prepareMsg.getReplica());
		String prepareKey = getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
		String prepareNumKey = getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
		Map<Long, MsgWithSign> value = prepareMsgs.compute(prepareKey, (k, v) -> {
			if (v == null) {
				v = Maps.newConcurrentMap();
			}
			if (v.putIfAbsent(Long.parseLong(prepareMsgWithSign.getId()), prepareMsgWithSign) == null) {
				logger.debug("add msg {}", prepareMsg);
			}
			else {
				logger.debug("has accepted the prepare msg, {}", prepareMsg);
			}
			return v;
		});
		if (value.size() >= 2 * f) {
			checkIsPrepared(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
		}
		// 以下为数据持久化的版本
//		String value = prepareNum.compute(prepareNumKey, (k, v) -> {
//			long num = 0; // v 为 null 时默认 num 为0
//			if (v == null || (v != null && (num = Long.parseLong(v)) < 2 * f)) {
//				if (prepareMsgs.putIfAbsent(prepareKey, prepareMsgWithSign.toString()) == null) {
//					logger.debug("add msg {}", prepareMsg);
//					num++;
//					return Long.toString(num);
//				}
//				else {
//					logger.debug("has accepted the prepare msg, {}", prepareMsg);
//				}
//			}
//			else {
//				logger.debug("has accepted >=2f (={}) prepare msgs", 2 * f);
//			}
//			return v;
//		});
//		if (Long.parseLong(value) >= 2 * f) {
//			checkIsPrepared(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
//		}
	}
	
	private void addCommit(MsgWithSign commitMsgWithSign) throws Exception{
		CommitMsg commitMsg = commitMsgWithSign.getCommitMsg();
//		String commitKey = getKey(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock(), commitMsg.getReplica());
		String commitKey = getKey(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
		String commitNumKey = getKey(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
		Map<Long, MsgWithSign> value = commitMsgs.compute(commitKey, (k, v) -> {
			if (v == null) {
				v = Maps.newConcurrentMap();
			}
			if (v.putIfAbsent(Long.parseLong(commitMsgWithSign.getId()), commitMsgWithSign) == null) {
				logger.debug("add msg {}", commitMsg);
			}
			else {
				logger.debug("has accepted the commit msg, {}", commitMsg);
			}
			return v;
		});
		if (value.size() >= 2 * f + 1) {
			checkIsCommitted(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
		}
		// 以下为数据持久化的版本
//		String value = commitNum.compute(commitNumKey, (k, v) -> {
//			long num = 0; // v 为 null 时默认 num 为0
//			if (v == null || (v != null && (num = Long.parseLong(v)) < 2 * f + 1)) {
//				if (commitMsgs.putIfAbsent(commitKey, commitMsgWithSign.toString()) == null) {
//					logger.debug("add msg {}", commitMsg);
//					num++;
//					return Long.toString(num);
//				}
//				else {
//					logger.debug("has accepted the commit msg, {}", commitMsg);
//				}
//			}
//			else {
//				logger.debug("has accepted >=2f+1 (={}) commit msgs", 2 * f + 1);
//			}
//			return v;
//		});
//		if (Long.parseLong(value) >= 2 * f + 1) {
//			checkIsCommitted(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
//		}
	}
	
	private void addPrepareCert(MsgWithSign prepareCertMsgWithSign) throws Exception{
		PrepareCertificate prepareCert = prepareCertMsgWithSign.getPrepareCertificate();
		PrepareMsg prepareMsg = prepareCert.getPrepareMsg();
		String prepareKey = getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
		MsgWithSign value = prepareCerts.putIfAbsent(prepareKey, prepareCertMsgWithSign);
		if (value == null) {
			logger.debug("add cert {}", prepareMsg);
			checkIsPrepared(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
		}
	}
	
	private void addCommitCert(MsgWithSign commitCertMsgWithSign) throws Exception{
		CommitCertificate commitCert = commitCertMsgWithSign.getCommitCertificate();
		CommitMsg commitMsg = commitCert.getCommitMsg();
		String commitKey = getKey(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
		MsgWithSign value = commitCerts.putIfAbsent(commitKey, commitCertMsgWithSign);
		if (value == null) {
			logger.debug("add cert {}", commitMsg);
			checkIsCommitted(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
		}
	}
	
	public void checkIsPrepared(long seqNum, long view, byte[] digest) throws Exception {
		String key = getKey(seqNum, view, digest);
		if (Status.COMMITED.toString().equals(statusMap.get(key))) {
			return;
		}
//		if (prepareNum.get(key) != null && Long.parseLong(prepareNum.get(key)) >= 2 * f) {
		if ((prepareMsgs.get(key) != null && prepareMsgs.get(key).size() >= 2 * f) || prepareCerts.get(key) != null) {
			// 若状态为PRE_PREPARED，则切换为PREPARED状态，并进入commit阶段；否则不切换状态
			if (statusMap.replace(key, Status.PRE_PREPARED.toString(), Status.PREPARED.toString())) {
				logger.debug("enter commit phase");
				handler.enterCommit(seqNum, view, digest);
				checkIsCommitted(seqNum, view, digest);
			}
		}
	}
	
	public void checkIsCommitted(long seqNum, long view, byte[] digest) throws Exception {
		String key = getKey(seqNum, view, digest);
		if (Status.COMMITED.toString().equals(statusMap.get(key))) {
			return;
		}
//		if (Long.parseLong(commitNum.get(key)) >= 2 * f + 1) {
		try {
			if ((commitMsgs.get(key) != null && commitMsgs.get(key).size() >= 2 * f + 1) || commitCerts.get(key) != null) {
				// 若状态为PREPARED，则切换为PREPARED状态，并进入commit阶段；否则不切换状态
				if (statusMap.replace(key, Status.PREPARED.toString(), Status.COMMITED.toString())) {
//					String dataString = prePrepareMsgs.get(getKey(seqNum, view));
//					byte[] data = dataString.getBytes(Charset.forName("ISO-8859-1"));
//					MsgWithSign msgWithSign = new MsgWithSign(data);
					MsgWithSign msgWithSign = prePrepareMsgs.get(getKey(seqNum, view));
					PrePrepareMsg prePrepareMsg = msgWithSign.getPrePrepareMsg();
					BlockMsg blockMsg = prePrepareMsg.getBlockMsg();
					handler.commited(seqNum, view, digest, blockMsg);
					if (AppConfig.isMulSig()) {
						prepareCerts.remove(getKey(seqNum, view, digest));
						commitCerts.remove(getKey(seqNum, view, digest));
					}
					else {
						prePrepareMsgs.remove(getKey(seqNum, view));
						prepareMsgs.remove(getKey(seqNum, view, digest));
						commitMsgs.remove(getKey(seqNum, view, digest));
						statusMap.remove(getKey(seqNum, view, digest));
					}
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	// if and only if replica has inserted in its log: 
	// a pre-prepare(with request block) in view v with sequence number n, 
	// and 2f prepares from different backups that match the pre-prepare. 
	
	// if and only if prepared(view, seqNum, digest) is true and 
	// has accepted 2f + 1 commits (possibly including its own) from different replicas that match the pre-prepare; 
	// a commit matches a pre-prepare if they have the same view, sequence number, and digest. 

//	private String getKey(PrePrepareMsg prePrepareMsg){
//		return getKey(prePrepareMsg.getSeqNum(), prePrepareMsg.getView());
//	}
//	
//	private String getKey(PrepareMsg prepareMsg){
//		return getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock());
//	}
//	
//	private String getKey(CommitMsg commitMsg){
//		return getKey(commitMsg.getSeqNum(), commitMsg.getView(), commitMsg.getDigestOfBlock());
//	}
	
	public String getKey(long seqNum, long view){
		return String.format("%s=%d:%s=%d", SEQ_NUM, seqNum, VIEW, view);
	}
	
	public String getKey(long seqNum, long view, byte[] digest){
		return String.format("%s=%d:%s=%d:%s=%s", SEQ_NUM, seqNum, VIEW, view, DIGEST , Hex.encode(digest));
	}
	
	public String getKey(long seqNum, long view, byte[] digest, long replica){
		return String.format("%s=%d:%s=%d:%s=%s:%s=%d", SEQ_NUM, seqNum, VIEW, view, DIGEST , Hex.encode(digest), REPLICA, replica);
	}
	
}
