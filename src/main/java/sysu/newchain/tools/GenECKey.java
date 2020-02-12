package sysu.newchain.tools;

import sysu.newchain.common.crypto.ECKey;

public class GenECKey {
	
	public ECKey genECKey() {
		return new ECKey();
	}
	
	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			ECKey ecKey = new ECKey();
			System.out.println(
					"priKey: " + ecKey.getPriKeyAsBase58() 
					+ ", pubKey: " + ecKey.getPubKeyAsBase58()	
					+ ", address: " + ecKey.toAddress().getEncodedBase58());
		}
	}
}
