package sysu.newchain.consensus.pbft.msg.log;

import sysu.newchain.consensus.pbft.msg.BlockMsg;

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
	
	public void commited(long seqNum, BlockMsg blockMsg) throws Exception;
	
}