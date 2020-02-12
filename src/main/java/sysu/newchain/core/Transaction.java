package sysu.newchain.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.Utils;
import sysu.newchain.common.format.VarInt;

public class Transaction extends Serialize{
	Address from; 	// 发送方地址
	Address to;		// 接受方地址
	long amount;	// 金额
	String time = "";	// 发送时间，当两笔不同交易的其他字段相同时，此字段应不同
	byte[] sign = new byte[0];	// 发送方签名
	byte[] pubKey = new byte[0];	// 发送方公钥
	byte[] data = new byte[0];	// 附加数据
	
	byte[] hash = new byte[0]; 	// 交易hash，唯一标识一笔交易,可根据以上字段计算得出,不参与序列化
	
	// 以下字段为接收交易请求的客户端信息，不参与序列化
	byte[] clientPubKey = new byte[0]; 	// 客户端公钥（用于辨识客户端，校验其签名）
	byte[] clientSign = new byte[0]; 	// 客户端签名
	
	public Transaction() {
		// TODO Auto-generated constructor stub
	}
	
	public Transaction(Address from, Address to, long amount, String time,
			byte[] sign, byte[] pubKey, byte[] data) {
		this.from = from;
		this.to = to;
		this.amount = amount;
		this.time = time;
		this.sign = sign;
		this.pubKey = pubKey;
		this.data = data;
	}
	
	public Transaction(Address from, Address to, long amount, String time,
			byte[] sign, byte[] pubKey, byte[] data, byte[] clientPubKey) {
		this(from, to, amount, time, sign, clientPubKey, data);
		this.clientPubKey = clientPubKey;
	}
	
	public Transaction(byte[] payload) throws Exception {
		super(payload);
	}
	
	public Transaction(byte[] payload, int offset) throws Exception {
		super(payload, offset);
	}
	
	public Address getFrom() {
		return from;
	}

	public void setFrom(Address from) {
		this.from = from;
	}

	public Address getTo() {
		return to;
	}

	public void setTo(Address to) {
		this.to = to;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public byte[] getSign() {
		return sign;
	}

	public void setSign(byte[] sign) {
		this.sign = sign;
	}

	public byte[] getPubKey() {
		return pubKey;
	}

	public void setPubKey(byte[] pubKey) {
		this.pubKey = pubKey;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	public byte[] getClientPubKey() {
		return clientPubKey;
	}

	public void setClientPubKey(byte[] clientPubKey) {
		this.clientPubKey = clientPubKey;
	}
	
	public byte[] getClientSign() {
		return clientSign;
	}

	public void setClientSign(byte[] clientSign) {
		this.clientSign = clientSign;
	}

	public byte[] calculateHash(){
		byte[] hash = null;
		try {
			hash = Hash.SHA256.hashTwice(this.serialize(SerializeType.TOHASH));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hash;
	}
	
	public byte[] calculateAndSetHash(){
		byte[] hash = this.calculateHash();
		this.setHash(hash);
		return hash;
	}
	
	public long calculateSize() throws Exception {
		return this.serialize().length;
	}

	@Override
	public void serializeToStream(OutputStream stream) throws Exception {
		this.serializeToStream(stream, SerializeType.ALL);
	}
	
	public void serializeToStream(OutputStream stream, SerializeType type) throws Exception {
		// from
		byte[] from = Base58.decode(this.from.getEncodedBase58());
		writeByteArray(from, stream);
		//to
		byte[] to = Base58.decode(this.to.getEncodedBase58());
		writeByteArray(to, stream);
		// amount
		stream.write(new VarInt(this.amount).encode());
		// time
		writeString(time, stream);
		if (SerializeType.ALL.equals(type)) {
			// sign
			writeByteArray(this.sign, stream);
		}
		// pubKey
		writeByteArray(this.pubKey, stream);
		// data
		writeByteArray(data, stream);
	}

	public byte[] serialize(SerializeType type) throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		this.serializeToStream(stream, type);
		return stream.toByteArray();
	}
	
	@Override
	protected void deserialize() throws Exception {
		this.cursor = offset;
		from = new Address(Base58.encode(readByteArray()));
		to = new Address(Base58.encode(readByteArray()));
		amount = readVarInt();
		time = readString();
		sign = readByteArray();
		pubKey = readByteArray();
		data = readByteArray();
		this.length = this.cursor - this.offset;
	}
	
	public enum SerializeType{
		ALL,
		TOHASH
	}
}