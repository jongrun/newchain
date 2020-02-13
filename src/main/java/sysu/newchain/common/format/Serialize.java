package sysu.newchain.common.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @Description 继承此类可方便将对象序列化和反序列化
 * @author jongliao
 * @date 2020年1月20日 上午10:30:02
 */
public abstract class Serialize{
	
	protected byte[] payload;
    // The offset is how many bytes into the provided byte array this payload starts at.
    protected int offset;
	// The cursor keeps track of where we are in the byte array as we parse it.
    protected int cursor;
    protected int length = Integer.MIN_VALUE;
	
    public Serialize() {
		// TODO Auto-generated constructor stub
	}
    
	public Serialize(byte[] payload) throws Exception {
		this(payload, 0);
	}
	
	public Serialize(byte[] payload, int offset) throws Exception {
		this.payload = payload;
		this.cursor = this.offset = offset;
		this.deserialize();
	}
	
	public abstract void serializeToStream(OutputStream stream) throws Exception;
	
	public byte[] serialize() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		this.serializeToStream(stream);
		byte[] buf = stream.toByteArray();
		this.length = buf.length;
		return buf;
	}
	
	protected abstract void deserialize() throws Exception;
	
    protected long readUint32(){
        long u = Utils.readUint32(payload, cursor);
        cursor += 4;
        return u;
    }
	
    protected long readInt64(){
        long u = Utils.readInt64(payload, cursor);
        cursor += 8;
        return u;
    }
    
    protected long readVarInt(){
        return readVarInt(0);
    }

    protected long readVarInt(int offset){
        VarInt varint = new VarInt(payload, cursor + offset);
        cursor += offset + varint.getOriginalSizeInBytes();
        return varint.value;
    }
    

    protected byte[] readBytes(int length){
        byte[] b = new byte[length];
        System.arraycopy(payload, cursor, b, 0, length);
        cursor += length;
        return b;
    }
    
    protected byte[] readByteArray(){
        long len = readVarInt();
        return readBytes((int)len);
    }
    
    protected String readString(){
        long length = readVarInt();
        return length == 0 ? "" : Utils.toString(readBytes((int) length), "UTF-8"); // optimization for empty strings
    }
    
    public void writeByteArray(byte[] bytes, OutputStream stream) throws IOException {
		stream.write(new VarInt(bytes.length).encode());
		stream.write(bytes);
    }
    
    public void writeString(String str, OutputStream stream) throws IOException{
    	if(str == null)
    		str = "";
    	byte[] strBytes = str.getBytes();
    	writeByteArray(strBytes, stream);
    }
    
    public final int getLength() {
        if (length == Integer.MIN_VALUE) {
        	try {
				byte[] bytes = serialize();
				length = bytes.length;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }
        return length;
    }
    
}