package sysu.newchain.common.format;

/**
 * @Description 十六进制编码解码：字节数组<-<解码--编码>->十六进制字符串
 * @author jongliao
 * @date 2020年1月20日 上午10:26:41
 */
public class Hex {
	
	public static String encode(byte[] input) {
		return new String(org.bouncycastle.util.encoders.Hex.encode(input));
	}
	
	public static byte[] decode(String input) {
		return org.bouncycastle.util.encoders.Hex.decode(input);
	}
}