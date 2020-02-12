package sysu.newchain.proto;

import sysu.newchain.common.proto.BlockPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.core.Block;

public class BlockPbTest {
	public static void main(String[] args) {
		Block block = new Block();
		BlockPbCloner cloner = (BlockPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.BLOCK);
		BlockPb blockPb = cloner.toProto(block);
	}
}
