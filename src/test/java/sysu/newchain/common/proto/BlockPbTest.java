package sysu.newchain.common.proto;

import sysu.newchain.common.core.Block;
import sysu.newchain.common.proto.BlockPbCloner;
import sysu.newchain.common.proto.ProtoClonerFactory;
import sysu.newchain.common.proto.ProtoClonerFactory.ProtoClonerType;
import sysu.newchain.proto.BlockPb;

public class BlockPbTest {
	public static void main(String[] args) {
		Block block = new Block();
		BlockPbCloner cloner = (BlockPbCloner) ProtoClonerFactory.getCloner(ProtoClonerType.BLOCK);
		BlockPb blockPb = cloner.toProto(block);
	}
}
