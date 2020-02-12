package sysu.newchain.core;

import java.io.OutputStream;

import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.VarInt;

public class BlockHeader extends Serialize{
	private long height;
	private byte[] prehash = new byte[0];
	private byte[] hash = new byte[0];
	private String time = "";
	
	public BlockHeader() {
		// TODO Auto-generated constructor stub
	}
	
	public BlockHeader(byte[] payload) throws Exception{
		super(payload);
	}
	
	public BlockHeader(byte[] payload, int offset) throws Exception{
		super(payload, offset);
	}
	
	public long getHeight() {
		return height;
	}
	
	public void setHeight(long height) {
		this.height = height;
	}

	public byte[] getPrehash() {
		return prehash;
	}

	public void setPrehash(byte[] prehash) {
		this.prehash = prehash;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	@Override
	public void serializeToStream(OutputStream stream) throws Exception {
		stream.write(new VarInt(height).encode());
		writeByteArray(prehash, stream);
		writeByteArray(hash, stream);
		writeString(time, stream);
	}

	@Override
	protected void deserialize() throws Exception {
		this.cursor = this.offset;
		height = readVarInt();
		prehash = readByteArray();
		hash = readByteArray();
		time = readString();
		this.length = this.cursor - this.offset;
	}
	
}
