package sysu.newchain.common.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.ECKey.ECDSASignature;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;

public class ECKeyTest {
	static Logger logger = LoggerFactory.getLogger(ECKeyTest.class);
	
	public static void main(String[] args) throws Exception {
		byte[] data = Hex.decode("2e593a3686df312cc9465e10105ed054836f5673e676bb6a119b305f6ee713b4");
		ECKey ecKey = ECKey.fromPrivate(Base58.decode("FPBKqEX7rDLXC1NLMfQiYxFSfc8AsEAFbJeuEmAcJPUT"));
		ECDSASignature signature = ecKey.sign(data);
		byte[] signBytes = signature.encodeToDER();
		System.out.println(Hex.encode(signBytes));
		System.out.println(Hex.encode(signBytes).equals("3044022049fc49d696fb81433d0f197e691f8286ddf329375526e4bb4b3b0223a4b9d209022072c9d863867b98468d1bef80abd96c7d38d4302cf5b9c8f3549f0bfd7d0afe27"));
		System.out.println(ecKey.verify(data, signature));
		logger.debug("debug");
		logger.error("error");
	}
}
