package sysu.newchain.common.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * @Description 用于哈希运算
 * @author jongliao
 * @date 2020年1月20日 上午10:25:01
 */
public enum Hash {
	
	SHA256("SHA-256"),
	RIPEMD160("RipeMD160");
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	private final ThreadLocal<MessageDigest> THREAD_LOCAL = new ThreadLocal<MessageDigest>();
	private final String algorithm;
	
	private Hash(String algorithm) {
		this.algorithm = algorithm;
	}
	
	
	public byte[] hash(byte[] input) {
		return hash(input, 0, input.length);
	}
	
	public byte[] hash(byte[] input, int offset, int length) {
		MessageDigest digest = getDigest();
		digest.update(input, offset, length);
		return digest.digest();
	}
	
	public byte[] hashTwice(byte[] input) {
		return hashTwice(input, 0, input.length);
	}
	
	public byte[] hashTwice(byte[] input, int offset, int length) {
		MessageDigest digest = getDigest();
		digest.update(input, offset, length);
		return digest.digest(digest.digest());
	}
	
	public MessageDigest getDigest() {
		MessageDigest messageDigest;
		if ((messageDigest = THREAD_LOCAL.get()) == null) {
			try {
				messageDigest = MessageDigest.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			THREAD_LOCAL.set(messageDigest);
		}
		return messageDigest;
	}
}
