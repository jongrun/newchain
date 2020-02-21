package sysu.newchain.consensus.server.pbft;

import sysu.newchain.consensus.server.pbft.msg.BlockMsg;

/** pbft共识完成后的处理
 * @author jongliao
 * @date: 2020年2月21日 上午9:57:23
 */
public interface PbftHandler {
	public void committed(long seqNum, long view, BlockMsg blockMsg) throws Exception;
}
