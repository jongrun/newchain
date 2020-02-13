package sysu.newchain.consensus.pbft.msg.log;

public interface PhaseShiftHandler {
	
	public enum Status{
		PRE_PREPARED,
		PREPARED,
		COMMITED
	}
	
	public void enterPrepare(long view, long seqNum, byte[] digest);
	
	public void enterCommit(long view, long seqNum, byte[] digest);
	
}