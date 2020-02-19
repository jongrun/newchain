package sysu.newchain.core;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.noneDSA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.VarInt;
import sysu.newchain.core.Transaction.Respone;

public class Block extends Serialize{
	private static final Logger logger = LoggerFactory.getLogger(Block.class);
	private BlockHeader header;
	private List<Transaction> transactions;
	
	public Block() {
		header = new BlockHeader();
		transactions = new ArrayList<Transaction>();
	}
	
	public Block(byte[] payload) throws Exception {
		super(payload);
	}
	
	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}
	
	public BlockHeader getHeader() {
		return header;
	}

	public void setHeader(BlockHeader header) {
		this.header = header;
	}
	
	public void addTransaction(Transaction tx) {
		if (transactions == null) {
			transactions = new ArrayList<Transaction>();
		}
		transactions.add(tx);
	}
	
	@Override
	public void serializeToStream(OutputStream stream) throws Exception {
		this.header.serializeToStream(stream);
		if (transactions != null) {
			stream.write(new VarInt(transactions.size()).encode());
			for (Transaction tx : transactions) {
				tx.serializeToStream(stream);
			}
		}
		else {
        	stream.write(new VarInt(0).encode());
        }
	}

	@Override
	protected void deserialize() throws Exception {
		this.cursor = this.offset;
		this.header = new BlockHeader(payload, this.cursor);
		this.cursor += this.header.getLength();
		int transNum = (int) readVarInt();
		transactions = new ArrayList<Transaction>(transNum);
		for (int i = 0; i < transNum; i++) {
			Transaction tx = new Transaction(payload, this.cursor);
			transactions.add(tx);
			this.cursor += tx.getLength();
		}
		this.length = this.cursor - this.offset;
	};
	
	public byte[] getHash() {
		return header.getHash();
	}
	
	public byte[] calculateHash() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		byte[] height = new VarInt(header.getHeight()).encode();
		stream.write(height, 0, height.length);
		stream.write(header.getTime().getBytes(), 0, header.getTime().getBytes().length);
		stream.write(header.getPrehash(), 0, header.getPrehash().length);
		for(Transaction tx : transactions) {
			if (tx.isOk()) {
				stream.write(tx.calculateHash(), 0, tx.getHash().length);
			}
		}
		byte[] bytesToHash = stream.toByteArray();
		return Hash.SHA256.hashTwice(bytesToHash);
	}
	
	public byte[] calculateAndSetHash() {
		byte[] hash = calculateHash();
		this.header.setHash(hash);
		return hash;
	}
	
	public void verifyTransactions(){
		for (Transaction tx: transactions) {
			// 检查hash是否相等
			if (!Arrays.equals(tx.calculateHash(), tx.getHash())) {
				logger.info(Respone.TX_HASH_ERROR.getMsg());
				tx.setResponse(Respone.TX_HASH_ERROR);
			}
			// 检查发送方签名
			else if (!tx.verifySign()) {
				logger.info(Respone.TX_SIGN_ERROR.getMsg());
				tx.setResponse(Respone.TX_SIGN_ERROR);
			}
		}
	}
}
