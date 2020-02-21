package sysu.newchain.consensus.server.pbft.msg;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.consensus.server.pbft.msg.MsgWithSign;
import sysu.newchain.consensus.server.pbft.msg.PrePrepareMsg;
import sysu.newchain.consensus.server.pbft.msg.PrepareMsg;
import sysu.newchain.tools.GenBlock;

public class MsgWithSignTest {
	
	private static void testPrePrepare() throws Exception {
		MsgWithSign msgWithSign = new MsgWithSign();
		PrePrepareMsg prePrepare = new PrePrepareMsg();
		prePrepare.setView(0);
		prePrepare.setSeqNum(0);
		prePrepare.setBlock(GenBlock.genBlock());
		prePrepare.calculateAndSetDigestOfBlock();
		msgWithSign.setPrePrepareMsg(prePrepare);
		ECKey ecKey = new ECKey();
		msgWithSign.calculateAndSetSign(ecKey);
		System.out.println(msgWithSign.verifySign(ecKey.getPubKeyAsBytes()));
	}
	
	private static void testPrepare() throws Exception {
		MsgWithSign msgWithSign = new MsgWithSign();
		PrepareMsg prepare = new PrepareMsg();
		prepare.setView(0);
		prepare.setSeqNum(0);
		prepare.setReplica(0);
		msgWithSign.setPrepareMsg(prepare);
		ECKey ecKey = new ECKey();
		msgWithSign.calculateAndSetSign(ecKey);
		System.out.println(msgWithSign.verifySign(ecKey.getPubKeyAsBytes()));
	}
	
	public static void main(String[] args) throws Exception {
		// pre-prepare
		testPrePrepare();
		// prepare
		testPrepare();
	}
}
