package sysu.newchain.sometest.map;

import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;





import sysu.newchain.consensus.server.pbft.msg.log.PhaseShiftHandler.Status;

import com.google.common.collect.Maps;

public class ConcurrentMapTest {
	public static void main(String[] args) {
		ConcurrentMap<byte[], byte[]> statusMap = Maps.newConcurrentMap();
		String key = "n_1_v_1_d_1";
		Status status = Status.PRE_PREPARED;
		byte[] keyBytes = key.getBytes();
		byte[] valueBytes = status.toString().getBytes();
		// 若为空，置为PRE_PREPARED
//		statusMap.putIfAbsent(keyBytes, valueBytes);
		// 若为PRE_PREPARED，置为PREPARED
//		statusMap.computeIfPresent(keyBytes, (k, v)->{
//			if (Arrays.equals(Status.PRE_PREPARED.toString().getBytes(), v)) {
//				return Status.PREPARED.toString().getBytes();
//			}
//			return v;
//		});
		if(!statusMap.replace(keyBytes, Status.PRE_PREPARED.toString().getBytes(), Status.PREPARED.toString().getBytes())){
			System.out.println("hello");
		};
		byte[] v3 = statusMap.putIfAbsent(keyBytes, valueBytes);
		byte[] v1 = statusMap.computeIfAbsent(keyBytes, k->{
			return valueBytes;
		});
		byte[] v2 = statusMap.computeIfAbsent(keyBytes, k->{
			return valueBytes;
		});
		System.out.println(Status.fromBytes(v1));
		System.out.println(Status.fromBytes(v2));
		if (v3 == null) {
			System.out.println("null");
		}
		else {
			System.out.println(Status.fromBytes(v3));
		}
	}
}
