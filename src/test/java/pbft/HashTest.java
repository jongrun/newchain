package pbft;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Hex;

public class HashTest {
	public static void main(String[] args) {
		String input = "hello";
		System.out.println(Hex.encode(Hash.RIPEMD160.hash(input.getBytes())));
		System.out.println(Hex.encode(Hash.SHA256.hash(input.getBytes())));
		
		System.out.println(Hex.encode(Hash.SHA256.hash(Hash.SHA256.hash(input.getBytes()))));
		System.out.println(Hex.encode(Hash.SHA256.hashTwice(input.getBytes())));
		
		System.out.println(Hex.encode(Hash.RIPEMD160.hash(Hash.RIPEMD160.hash(input.getBytes()))));
		System.out.println(Hex.encode(Hash.RIPEMD160.hashTwice(input.getBytes())));
	}
}
