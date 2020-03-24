package sysu.newchain.rpc.dto;

import sysu.newchain.common.core.BlockHeader;
import sysu.newchain.common.format.Hex;

public class BlockHeaderDTO {
	String hash;
	String preHash;
	long height;
	String time;
	
	public static BlockHeaderDTO fromObject(BlockHeader blockHeader){
		if (blockHeader == null) {
			return null;
		}
		BlockHeaderDTO blockHeaderDTO = new BlockHeaderDTO();
		blockHeaderDTO.hash = Hex.encode(blockHeader.getHash());
		blockHeaderDTO.preHash = Hex.encode(blockHeader.getPrehash());
		blockHeaderDTO.height = blockHeader.getHeight();
		blockHeaderDTO.time = blockHeader.getTime();
		return blockHeaderDTO;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getPreHash() {
		return preHash;
	}

	public void setPreHash(String preHash) {
		this.preHash = preHash;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
}
