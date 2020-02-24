package sysu.newchain.tools;

import sysu.newchain.common.crypto.SchnorrKey;

public class GenECKey {
	
	public SchnorrKey genECKey() {
		return new SchnorrKey();
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			SchnorrKey ecKey = new SchnorrKey();
			System.out.println(
					"priKey: " + ecKey.getPriKeyAsBase58() 
					+ ", pubKey: " + ecKey.getPubKeyAsBase58()	
					+ ", address: " + ecKey.toAddress().getEncodedBase58());
		}
	}
}
