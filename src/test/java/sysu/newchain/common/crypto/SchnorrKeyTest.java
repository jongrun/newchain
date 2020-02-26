package sysu.newchain.common.crypto;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.crypto.ECKey;
import sysu.newchain.common.crypto.SchnorrKey.SchnorrSignature;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Utils;

public class SchnorrKeyTest {
	private final static Logger logger = LoggerFactory.getLogger(SchnorrKeyTest.class);
	
	public static void main(String[] args) throws Exception {
		String data = "hello";
		String data2 = "hello2";
		SchnorrKey key = new SchnorrKey();
		SchnorrKey key2 = new SchnorrKey();
//		logger.debug("pubKey: {}", key.getPubKeyAsBase58());
//		logger.debug("priKey: {}", key.getPriKeyAsBase58());
		byte[] hash = Hash.SHA256.hash(data.getBytes());
		byte[] hash2 = Hash.SHA256.hash(data2.getBytes());
		byte[] sign = key.sign(hash).toByteArray();
		byte[] sign2 = key2.sign(hash).toByteArray();
		byte[] sign3 = key2.sign(hash2).toByteArray();
		byte[] mulSign = SchnorrSignature.aggregateSign(new ArrayList<byte[]>(){{add(sign);add(sign2);}}).toByteArray();
		logger.debug("sign: {}", Hex.encode(sign));
		logger.debug("sign2: {}", Hex.encode(sign2));
		logger.debug("sign3: {}", Hex.encode(sign3));
		logger.debug("mulSign: {}", Hex.encode(mulSign));
		logger.debug("verify sign: {}", key.verify(hash, sign));
		logger.debug("verify sign2: {}", key2.verify(hash, sign2));
		logger.debug("verify sign3: {}", key2.verify(hash2, sign3));
		logger.debug("verify mulSign: {}", SchnorrKey.verifyMulSig(hash, mulSign, new ArrayList<byte[]>(){{add(key.getPubKeyAsBytes());add(key2.getPubKeyAsBytes());}}));
		List<byte[]> datas = new ArrayList<byte[]>();
		datas.add(hash);
		datas.add(hash2);
		List<byte[]> mulSign2 = new ArrayList<byte[]>();
		mulSign2.add(sign);
		mulSign2.add(sign3);
//		List<byte[]> hash
		logger.debug("verify mulSign of diff data: {}", SchnorrKey.verify(datas, mulSign2, new ArrayList<byte[]>(){{add(key.getPubKeyAsBytes());add(key2.getPubKeyAsBytes());}}));
	}
}
