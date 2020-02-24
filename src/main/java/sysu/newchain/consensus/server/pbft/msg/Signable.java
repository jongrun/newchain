package sysu.newchain.consensus.server.pbft.msg;

import sysu.newchain.common.crypto.SchnorrKey;

/** 可验签消息的接口
 * @author jongliao
 * @date: 2020年2月15日 上午9:59:02
 */
public interface Signable {
	public byte[] calculateSign(SchnorrKey ecKey) throws Exception;
	public void calculateAndSetSign(SchnorrKey ecKey) throws Exception;
	public boolean verifySign(byte[] pubKey);
}
