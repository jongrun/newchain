package sysu.newchain.tools;

import sysu.newchain.common.core.Address;
import sysu.newchain.common.core.Block;
import sysu.newchain.common.core.BlockHeader;
import sysu.newchain.common.core.Transaction;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;

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
