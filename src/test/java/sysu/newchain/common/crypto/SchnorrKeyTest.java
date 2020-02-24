package sysu.newchain.common.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Utils;

public class SchnorrKeyTest {
	private final static Logger logger = LoggerFactory.getLogger(SchnorrKeyTest.class);
	
	public static void main(String[] args) throws Exception {
		ECKey key = new ECKey();
		logger.debug("pubKey: {}", key.getPubKeyAsBase58());
		logger.debug("priKey: {}", key.getPriKeyAsBase58());
		String data = "hello";
		byte[] hash = Hash.SHA256.hash(data.getBytes());
		byte[] sign = key.sign(hash).toByteArray();
		logger.debug("hash to sign: {}", Hex.encode(hash));
		logger.debug("sign: {}", Hex.encode(sign));
		logger.debug("verify: {}", key.verify(hash, sign));
	}
}
