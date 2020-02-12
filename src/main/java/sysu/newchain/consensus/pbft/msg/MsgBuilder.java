package sysu.newchain.consensus.pbft.msg;

public abstract class MsgBuilder<Builder> {
	private Builder builder;
	
	public Builder getBuilder() {
		return builder;
	}
	
	public void setBuilder(Builder builder) {
		this.builder = builder;
	}
	
	public abstract byte[] toByteArray();
}
