package sysu.newchain.consensus.pbft;

import sysu.newchain.consensus.pbft.msg.BlockMsg;

public interface PbftHandler {
	public void committed(long seqNum, long view, BlockMsg blockMsg) throws Exception;
}
