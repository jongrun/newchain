package sysu.newchain.rpc.dto;

import java.util.ArrayList;
import java.util.List;

import sysu.newchain.core.Block;
import sysu.newchain.core.Transaction;

public class BlockDTO {
	BlockHeaderDTO header;
	List<TxDTO> transactions;
	
	public static BlockDTO fromObject(Block block){
		if (block == null) {
			return null;
		}
		BlockDTO blockDTO = new BlockDTO();
		BlockHeaderDTO headerDTO = BlockHeaderDTO.fromObject(block.getHeader());
		List<TxDTO> transactions = new ArrayList<TxDTO>(block.getTransactions().size());
		for(Transaction tx : block.getTransactions()){
			transactions.add(TxDTO.fromObject(tx));
		}
		blockDTO.header = headerDTO;
		blockDTO.transactions = transactions;
		return blockDTO;
	}

	public BlockHeaderDTO getHeader() {
		return header;
	}

	public void setHeader(BlockHeaderDTO header) {
		this.header = header;
	}

	public List<TxDTO> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<TxDTO> transactions) {
		this.transactions = transactions;
	}
	
}
