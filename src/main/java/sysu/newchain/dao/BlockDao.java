package sysu.newchain.dao;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.DataBase;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Serialize;
import sysu.newchain.common.format.Utils;
import sysu.newchain.common.format.VarInt;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;

public class BlockDao extends DataBase{
	private static final Logger logger = LoggerFactory.getLogger(BlockDao.class);
	
	static final String DBNAME = "block.db";
	
	private static final BlockDao instance = new BlockDao();
	
	public static BlockDao getInstance() {
		return instance;
	}
	
	private TransactionDao txDao = TransactionDao.getInstance(); 
	
	private BlockDao() {
		super(DBNAME);
	}
	
	@Override
	public void init() {
		super.init();
		try {
			createGenesisBlock();
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	public void createGenesisBlock() throws Exception{
		if (getLastHeight() < 0) {
			BlockHeader header = new BlockHeader();
			header.setHeight(0L);
			header.setTime(System.currentTimeMillis() + "");
			Block block = new Block();
			block.setHeader(header);
			block.calculateAndSetHash();
			insertBlock(block);
		}
	}
	
	public void insertBlock(Block block) throws Exception {
		StoredBlock storedBlock = new StoredBlock(block);
		rocksDB.put(Utils.longToBytes(block.getHeader().getHeight()), storedBlock.serialize());
		for (Transaction tx : block.getTransactions()) {
			if (tx.isOk()) {
				txDao.insertTransaction(tx);
			}
		}
		if (block.getHeader().getHeight() > getLastHeight()) {
			setLastHeight(block.getHeader().getHeight());
		}
	}
	
	public Block getBlock(long height) throws Exception {
		byte[] value = rocksDB.get(Utils.longToBytes(height));
		if (value != null) {
			StoredBlock storedBlock = new StoredBlock(value);
			Block block = new Block();
			block.setHeader(storedBlock.getHeader());
			for(byte[] hash : storedBlock.getTxHashes()){
				block.addTransaction(txDao.getTransaction(hash));
			}
			return block;
		}
		return null;
	}
	
	public BlockHeader getBlockHeader(long height) throws Exception{
		byte[] value = rocksDB.get(Utils.longToBytes(height));
		if (value != null) {
			StoredBlock storedBlock = new StoredBlock(value);
			return storedBlock.getHeader();
		}
		return null;
	}
	
	public void setLastHeight(long height) throws RocksDBException {
		rocksDB.put(Utils.longToBytes(-1), Utils.longToBytes(height));
	}
	
	public long getLastHeight() throws RocksDBException {
		byte[] value = rocksDB.get(Utils.longToBytes(-1));
		if (value != null) {
			return Utils.bytesToLong(value);
		}
		else {
			return -1L;
		}
	}
	
	public class StoredBlock extends Serialize{
		BlockHeader header;
		List<byte[]> txHashes; // 只存储交易hash，根据hash搜索交易
		
		public StoredBlock() {
			header = new BlockHeader();
			txHashes = new ArrayList<byte[]>();
		}
		
		public StoredBlock(byte[] data) throws Exception{
			super(data);
		}
		
		public StoredBlock(Block block){
			header = block.getHeader();
			txHashes = new ArrayList<byte[]>();
			for(Transaction tx : block.getTransactions()){
				if (tx.isOk()) {
					txHashes.add(tx.calculateHash());
				}
			}
		}
		
		public BlockHeader getHeader() {
			return header;
		}

		public void setHeader(BlockHeader header) {
			this.header = header;
		}

		public List<byte[]> getTxHashes() {
			return txHashes;
		}

		public void setTxHashes(List<byte[]> txHashes) {
			this.txHashes = txHashes;
		}

		@Override
		public void serializeToStream(OutputStream stream) throws Exception {
			this.header.serializeToStream(stream);
			if (txHashes != null) {
				stream.write(new VarInt(txHashes.size()).encode());
				for (byte[] hash : txHashes) {
					writeByteArray(hash, stream);
				}
			}
			else {
				logger.debug("txHashes == null");
	        	stream.write(new VarInt(0).encode());
	        }
		}
		
		@Override
		protected void deserialize() throws Exception {
			this.cursor = this.offset;
			this.header = new BlockHeader(payload, this.cursor);
			this.cursor += this.header.getLength();
			int transNum = (int) readVarInt();
			txHashes = new ArrayList<byte[]>(transNum);
			for (int i = 0; i < transNum; i++) {
				txHashes.add(readByteArray());
			}
			this.length = this.cursor - this.offset;
		}
	}
}
