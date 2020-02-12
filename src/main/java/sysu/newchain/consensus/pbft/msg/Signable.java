package sysu.newchain.consensus.pbft.msg;

import sysu.newchain.common.crypto.ECKey;

public interface Signable {
	public byte[] calculateSign(ECKey ecKey) throws Exception;
	public void calculateAndSetSign(ECKey ecKey) throws Exception;
	public boolean verifySign(byte[] pubKey);
}
