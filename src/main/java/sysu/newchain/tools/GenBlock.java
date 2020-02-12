package sysu.newchain.tools;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.core.Address;
import sysu.newchain.core.Block;
import sysu.newchain.core.BlockHeader;
import sysu.newchain.core.Transaction;

public class GenBlock {
	public static Block genBlock() throws Exception {
		BlockHeader header = new BlockHeader();
//		header.setHeight(1L);
		header.setTime("");
//		header.setPrehash(Hash.SHA256.hashTwice("hello".getBytes()));
		Block block = new Block();
		block.setHeader(header);
		block.addTransaction(GenTransaction.genTransaction());
		block.addTransaction(GenTransaction.genTransaction());
//		block.calculateAndSetHash();
		return block;
	}
}
