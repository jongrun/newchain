package sysu.newchain.sometest.map;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import sysu.newchain.common.format.Utils;
import sysu.newchain.consensus.server.pbft.msg.PrepareMsg;

public class MapComputeTest {
	static final Logger logger = LoggerFactory.getLogger(MapComputeTest.class);
	
	public static void main(String[] args) {
		ConcurrentHashMap<String, byte[]> prepareNum = new ConcurrentHashMap<String, byte[]>();
//		PrepareMsg prepareMsg = prepareMsgWithSign.getPrepareMsg();
//		String prepareKey = getKey(prepareMsg.getSeqNum(), prepareMsg.getView(), prepareMsg.getDigestOfBlock(), prepareMsg.getReplica());
		String prepareNumKey = "n=1:v=0:d=da3e207c167748e84e7adf92b7f3e181c75c274d6e85e307bf718e1fc11c3db9";
		byte[] value = prepareNum.compute(prepareNumKey, (k, v) -> {
			logger.debug("prepareNumKey: {}", prepareNumKey);
			logger.debug("v==null: {}", v==null);
			long num = 0; // v 为 null 时默认 num 为0
			if (v == null) {
				logger.debug("num before {}", num);
				num++;
				logger.debug("num after {}", num);
				return Utils.longToBytes(num);
			}
			logger.debug("test here");
			return v;
		});
	}
}
