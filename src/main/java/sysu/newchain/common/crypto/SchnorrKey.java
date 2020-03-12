package sysu.newchain.common.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.pqc.math.linearalgebra.IntegerFunctions;
import org.bouncycastle.util.BigIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sysu.newchain.common.crypto.SchnorrKey.SchnorrSignature;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Utils;
import sysu.newchain.core.Address;

public class SchnorrKey {
	private final static Logger logger = LoggerFactory.getLogger(SchnorrKey.class);
	
	private BigInteger priKey;
	private ECPoint pubKey;
	private byte[] pubKeyBytes;
	
	private static final ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
    private static final ECPoint G = ecSpec.getG();
    private static final BigInteger order = ecSpec.getN();
    private static final BigInteger p = Utils.bytesToBigInteger(Hex.decode("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F"));
    
	public SchnorrKey() {
		this.priKey = BigIntegers.createRandomInRange(BigInteger.ONE, order.subtract(BigInteger.ONE), new SecureRandom());
		this.pubKey = G.multiply(this.priKey).normalize();
	}
	
	public SchnorrKey(BigInteger priKey, ECPoint pubKey) {
		this.priKey = priKey;
		this.pubKey = pubKey;
	}
	
	public ECPoint getPubKey() {
		return pubKey;
	}
	
	public byte[] getPubKeyAsBytes() {
		if (pubKeyBytes == null) {
			pubKeyBytes = pubKey.getEncoded(true);
		}
		return pubKeyBytes;
	}
	
	public String getPubKeyAsBase58() {
		return Base58.encode(getPubKeyAsBytes());
	}
	
	public String getPubKeyAsHex() {
		return new String(Hex.encode(getPubKeyAsBytes()));
	}
	
	public BigInteger getPriKey() {
		return priKey;
	}
	
	public byte[] getPriKeyAsBytes() {
		if (priKey == null) {
			return null;
		}
		else {
			return Utils.bigIntegerToBytes(priKey, 32);
		}
	}
	
	public String getPriKeyAsBase58() {
		return Base58.encode(getPriKeyAsBytes());
	}
	
	public String getPriKeyAsHex() {
		return new String(Hex.encode(getPriKeyAsBytes()));
	}
	
	public byte[] getPubKeyHash() {
		return Hash.RIPEMD160.hash(Hash.SHA256.hash(getPubKeyAsBytes()));
	}
	
	public static SchnorrKey fromPrivate(BigInteger priKey) {
		ECPoint pubKey = G.multiply(priKey).normalize();
		return new SchnorrKey(priKey, pubKey);
	}
	
	public static SchnorrKey fromPrivate(byte[] priKeyBytes) {
		return fromPrivate(new BigInteger(1, priKeyBytes));
	}
	
	public static SchnorrKey fromPubKeyOnly(ECPoint pubKey) {
		return new SchnorrKey(null, pubKey);
	}
	
	public static SchnorrKey fromPubKeyOnly(byte[] pubKey) {
		return new SchnorrKey(null, decodePoint(pubKey));
	}
	
	public Address toAddress() throws Exception {
		return new Address(getPubKeyHash());
	}
	
	public SchnorrSignature sign(byte[] input) throws Exception {
		if (priKey == null) {
			throw new Exception("Missing private key!");
		}
//		BigInteger r = BigIntegers.createRandomInRange(BigInteger.ONE, order, new SecureRandom());
		// 随机数与私钥和消息联系起来，可以保证对同一条消息的签名相同 TODO 待评审
		BigInteger r = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(Utils.bigIntegerToBytes(priKey, 32), input))).mod(order);
		// R = rG
        ECPoint R = G.multiply(r).normalize();
        
        /* Checks if R is a quadratic residue (?) */
        while (jacobi(R.getAffineYCoord().toBigInteger()) != 1) {
            r = (order.subtract(r));
            R = G.multiply(r).normalize();
        }
        
//        // e = hash(R, P, m)
//        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(R.getEncoded(true), getPubKeyAsBytes(), input))).mod(order);
        
        // e = hash(m)
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(input)).mod(order);
        
        // s = r + ke
        BigInteger s = r.add(e.multiply(priKey)).mod(order);
        
        return new SchnorrSignature(R, s);
	}
	
    private static int jacobi(BigInteger x) {
        return IntegerFunctions.jacobi(x, p);
    }
	
    /** 
     * @param data 签名数据
     * @param sign 待验证签名
     * @param P 公钥
     * @return 验签是否成功
     */
    public static boolean verify(byte[] data, SchnorrSignature sign, ECPoint P) {
        if (!(sign.getR().getAffineXCoord().toBigInteger().compareTo(p) == -1)) {
            logger.error("Failed cuz Rx greater than curve modulus");
            return false;
        }

        if (!(sign.getS().compareTo(order) == -1)) {
        	logger.error("Failed cuz s greater than curve order");
            return false;
        }
        
        if (!ecSpec.getCurve().importPoint(P).isValid()) {
        	logger.error("Failed cuz invalid point");
            return false;
        }
    	
        /* s = r + ke
         * sG = (r + ke)G = R + Pe
         */
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(data)).mod(order);
        ECPoint S1 = G.multiply(sign.getS()).normalize();
        ECPoint S2 = sign.getR().add(P.multiply(e).normalize()).normalize();
        if (!(S1.getAffineXCoord().toBigInteger().compareTo(S2.getAffineXCoord().toBigInteger()) == 0)) {
        	logger.error("S1 != S2");
			return false;
		}
        return true;
    }
    
    public static boolean verify(byte[] data, byte[] sign, byte[] pubKey) {
        return verify(data, SchnorrSignature.fromByteArray(sign), decodePoint(pubKey));
    }
    
    /** 用此密钥的公钥验证签名
     * @param data 签名数据
     * @param sign 待验证签名
     * @return 验签是否成功
     */
    public boolean verify(byte[] data, SchnorrSignature sign) {
    	return SchnorrKey.verify(data, sign, getPubKey());
    }
    
    public boolean verify(byte[] data, byte[] sign) {
        return SchnorrKey.verify(data, sign, getPubKeyAsBytes());
    }
    
    /** 同时验证多个签名，将签名聚合为一个聚合签名，比逐个验证效率更高
     * @param datas 数据列表
     * @param signs 签名列表
     * @param Ps 公钥列表
     * @return 是否验签成功
     */
    private static boolean verifyMulSig(List<byte[]> datas, List<SchnorrSignature> signs, List<ECPoint> Ps) {
    	int size = datas.size();
    	if (size != signs.size() || size != Ps.size()) {
			return false;
		}
    	SchnorrSignature sign = SchnorrSignature.aggregate(signs);
    	logger.debug("mulSign: {}", Hex.encode(sign.toByteArray()));
		return verifyMulSig(datas, sign, Ps);
	}
    
    public static boolean verify(List<byte[]> datas, List<byte[]> signs, List<byte[]> pubKeys) {
    	int size = datas.size();
    	if (size != signs.size() || size != pubKeys.size()) {
    		logger.debug("size error");
			return false;
		}
    	List<SchnorrSignature> signatures = new ArrayList<SchnorrKey.SchnorrSignature>(size);
    	List<ECPoint> Ps = new ArrayList<ECPoint>(size);
    	for(int i = 0; i < signs.size(); i++){
    		signatures.add(SchnorrSignature.fromByteArray(signs.get(i)));
    		Ps.add(decodePoint(pubKeys.get(i)));
    	}
    	return verifyMulSig(datas, signatures, Ps);
    }
    
    /** 验证多个签名方对多个数据的聚合签名
     * @param datas 数据列表
     * @param sign 聚合签名
     * @param Ps 公钥列表
     * @return 验签是否成功
     */
    public static boolean verifyMulSig(List<byte[]> datas, SchnorrSignature sign, List<ECPoint> Ps){
    	if (datas.size() != Ps.size() || datas.size() < 1) {
			return false;
		}
    	/* s(i) = r(i) + k(i)e(i)
         * s(i)G = (r(i) + k(i)e(i))G = R(i) + P(i)e(i)
         */
    	ECPoint S1 = G.multiply(sign.getS()).normalize();
    	ECPoint S2 = sign.getR();
    	BigInteger e;
        for(int i = 0; i < Ps.size(); i++){
        	e = Utils.bytesToBigInteger(Hash.SHA256.hash(datas.get(i))).mod(order);
        	S2 = S2.add(Ps.get(i).multiply(e).normalize()).normalize();
        }
        if (!(S1.getAffineXCoord().toBigInteger().compareTo(S2.getAffineXCoord().toBigInteger()) == 0)) {
        	logger.error("S1 != S2");
			return false;
		}
        return true;
    }
    
    public static boolean verifyMulSig(List<byte[]> datas, byte[] sign, List<byte[]> pubKeys){
    	if (datas.size() != pubKeys.size() || datas.size() < 1) {
			return false;
		}
    	List<ECPoint> Ps = new ArrayList<ECPoint>(pubKeys.size());
    	for(byte[] pubKey : pubKeys){
    		Ps.add(decodePoint(pubKey));
    	}
    	return verifyMulSig(datas, SchnorrSignature.fromByteArray(sign), Ps);
    }
    
    /** 验证多个签名方对同一个数据的聚合签名
     * @param data 数据
     * @param sign 聚合签名
     * @param Ps 公钥列表
     * @return 验签是否成功
     */
    public static boolean verifyMulSig(byte[] data, SchnorrSignature sign, List<ECPoint> Ps){
    	if (Ps.size() < 1) {
			return false;
		}
    	ECPoint point = Ps.get(0);
    	for (int i = 1; i < Ps.size(); i++) {
    		point = point.add(Ps.get(i)).normalize();
		}
    	return verify(data, sign, point);
    }
    
    public static boolean verifyMulSig(byte[] data, byte[] sign, List<byte[]> pubKeys){
    	if (pubKeys.size() < 1) {
			return false;
		}
    	ECPoint point = decodePoint(pubKeys.get(0));
    	for (int i = 1; i < pubKeys.size(); i++) {
			point = point.add(decodePoint(pubKeys.get(i))).normalize();
		}
    	return verify(data, SchnorrSignature.fromByteArray(sign), point);
    }
    
    private static ECPoint decodePoint(byte[] pubKey) {
    	return ecSpec.getCurve().decodePoint(pubKey).normalize();
	}
    
	public static class SchnorrSignature{
		private ECPoint R;
		private BigInteger s;
		
		public SchnorrSignature(ECPoint R, BigInteger s) {
			super();
			this.R = R;
			this.s = s;
		}
		
		public ECPoint getR() {
			return R;
		}
		
		public BigInteger getS() {
			return s;
		}
		
		private byte[] getRBytes() {
			return R.getEncoded(true);
		}
		
		private byte[] getSBytes() {
			return Utils.bigIntegerToBytes(s, 32);
		}
		
		public byte[] toByteArray() {
			return Utils.merge(getRBytes(), getSBytes());
		}
		
		public static SchnorrSignature fromByteArray(byte[] data) {
			ECPoint R = decodePoint(Arrays.copyOfRange(data, 0, 33));
			BigInteger s = Utils.bytesToBigInteger(Arrays.copyOfRange(data, 33, 65));
			return new SchnorrSignature(R, s);
		}
		
		/** 将多个签名聚合为一个签名
		 * @param signs 待聚合签名列表
		 * @return 聚合签名
		 */
		public static SchnorrSignature aggregate(List<SchnorrSignature> signs){
			if (signs.size() < 1) {
				return null;
			}
			ECPoint R = signs.get(0).getR().normalize();
			BigInteger s = signs.get(0).getS();
			for (int i = 1; i < signs.size(); i++) {
				R = R.add(signs.get(i).getR()).normalize();
				s = s.add(signs.get(i).getS()).mod(order);
			}
			return new SchnorrSignature(R, s);
		}
		
		/** 将多个签名聚合为一个签名
		 * @param signs 待聚合签名列表
		 * @return 聚合签名
		 */
		public static SchnorrSignature aggregateSign(List<byte[]> signs){
			if (signs.size() < 1) {
				return null;
			}
			ECPoint R = SchnorrSignature.fromByteArray(signs.get(0)).getR().normalize();
			BigInteger s = SchnorrSignature.fromByteArray(signs.get(0)).getS();
			for (int i = 1; i < signs.size(); i++) {
				R = R.add(SchnorrSignature.fromByteArray(signs.get(i)).getR()).normalize();
				s = s.add(SchnorrSignature.fromByteArray(signs.get(i)).getS()).mod(order);
			}
			return new SchnorrSignature(R, s);
		}
	}
	
}
