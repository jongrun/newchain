package sysu.newchain.sometest;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import sysu.newchain.common.format.Hex;

public class StringTest {
	public static void main(String[] args) throws UnsupportedEncodingException {
		byte[] bytes = Hex.decode("1a91031001");
		String str = new String(bytes, Charset.forName("ISO-8859-1"));
		byte[] after = str.getBytes(Charset.forName("ISO-8859-1"));
		System.out.println("before: " + Hex.encode(bytes) + "--" + Arrays.toString(bytes));
		System.out.println("after : " + Hex.encode(after) + "--" + Arrays.toString(after));
		System.out.println(Charset.defaultCharset().name());
		
	}
}
