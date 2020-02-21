package sysu.newchain.consensus.server;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.format.Hex;
import sysu.newchain.consensus.server.pbft.msg.MsgWithSign;
import sysu.newchain.core.Transaction;

public class RequestReceiver extends ReceiverAdapter{
	static final Logger logger = LoggerFactory.getLogger(RequestReceiver.class);
	private BlockBuildManager blockBuildManager;
	
	public void init(){
		blockBuildManager = BlockBuildManager.getInstance();
	}
	
	@Override
	public void receive(Message msg) {
		logger.debug("get msg {}", msg);
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
				blockBuildManager.pushTransaction(tx);
				break;
			default:
				break;
			};
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
