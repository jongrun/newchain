package sysu.newchain.consensus.server.pbft.msg.log;

import sysu.newchain.consensus.server.pbft.msg.BlockMsg;

/** pbft算法 阶段转换 时需要做的操作，阶段转换在消息日志中触发
 * pre-prepared -- enter prepare phase -->
 * prepared -- enter commit phase -->
 * committed
 * @author jongliao
 * @date: 2020年2月21日 上午9:52:16
 */
public interface PhaseShiftHandler {
	
	public enum Status{
		PRE_PREPARED,
		PREPARED,
		COMMITED;
		public static Status fromBytes(byte[] data){
			return Status.valueOf(new String(data));
		}
		public byte[] toByteArray(){
			return this.toString().getBytes();
		}
	}
	
	public void enterPrepare(long seqNum, long view, byte[] digest) throws Exception;
	
	public void enterCommit(long seqNum, long view, byte[] digest) throws Exception;
	
	public void commited(long seqNum, long view, BlockMsg blockMsg) throws Exception;
	
}