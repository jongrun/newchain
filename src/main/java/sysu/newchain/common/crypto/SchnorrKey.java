package sysu.newchain.common.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
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
        
        // e = hash(R, P, m)
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(R.getEncoded(true), getPubKeyAsBytes(), input))).mod(order);
        
        // s = r + ke
        BigInteger s = r.add(e.multiply(priKey)).mod(order);
        
        return new SchnorrSignature(R, s);
	}
	
    private static int jacobi(BigInteger x) {
        return IntegerFunctions.jacobi(x, p);
    }
	
    public static boolean verify(byte[] data, SchnorrSignature signature, byte[] pubKey) {
        if (!(signature.getR().getAffineXCoord().toBigInteger().compareTo(p) == -1)) {
            logger.error("Failed cuz Rx greater than curve modulus");
            return false;
        }

        if (!(signature.getS().compareTo(order) == -1)) {
        	logger.error("Failed cuz s greater than curve order");
            return false;
        }
        
        ECPoint P = decodePoint(pubKey);
        
        if (!ecSpec.getCurve().importPoint(P).isValid()) {
        	logger.error("Failed cuz invalid point");
            return false;
        }
    	
        /* s = r + ke
         * sG = (r + ke)G = R + Pe
         */
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(signature.getRBytes(), pubKey, data))).mod(order);
        
        ECPoint S1 = G.multiply(signature.getS()).normalize();
        ECPoint S2 = signature.getR().add(P.multiply(e).normalize()).normalize();
        
        if (!(S1.getAffineXCoord().toBigInteger().compareTo(S2.getAffineXCoord().toBigInteger()) == 0)) {
        	logger.error("S1 != S2");
			return false;
		}
        return true;
    }
    
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return verify(data, SchnorrSignature.fromByteArray(signature), pub);
    }
    
    public boolean verify(byte[] data, SchnorrSignature signature) {
    	return SchnorrKey.verify(data, signature, getPubKeyAsBytes());
    }
    
    public boolean verify(byte[] data, byte[] signature) {
        return SchnorrKey.verify(data, signature, getPubKeyAsBytes());
    }
    
    private static boolean verifyMulSig(List<byte[]> datas, List<ECPoint> Ps, List<ECPoint> Rs, BigInteger sumOfS){
    	/* s(i) = r(i) + k(i)e(i)
         * s(i)G = (r(i) + k(i)e(i))G = R(i) + P(i)e(i)
         */
        
        ECPoint sumOfS1 = G.multiply(sumOfS).normalize();
        
    	ECPoint R0 = Rs.get(0);
    	ECPoint P0 = Ps.get(0);
    	byte[] data0 = datas.get(0);
    	BigInteger e0 = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(R0.getEncoded(true), P0.getEncoded(true), data0))).mod(order);
        ECPoint sumOfS2 = R0.add(P0.multiply(e0).normalize()).normalize();
        
        for(int i = 1; i < Ps.size(); i++){
        	ECPoint R = Rs.get(i);
        	ECPoint P = Ps.get(i);
        	byte[] data = datas.get(i);
        	BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(R.getEncoded(true), P.getEncoded(true), data))).mod(order);
        	sumOfS2.add(R.add(P.multiply(e).normalize()).normalize()).normalize();
        }
        
        if (!(sumOfS1.getAffineXCoord().toBigInteger().compareTo(sumOfS2.getAffineXCoord().toBigInteger()) == 0)) {
        	logger.error("S1 != S2");
			return false;
		}
        return true;
    }
    
    public static boolean verify(List<byte[]> datas, List<byte[]> pubKeys, List<byte[]> RBytes, BigInteger sumOfS){
    	int size = datas.size();
    	if (size != pubKeys.size() || size != RBytes.size()) {
			return false;
		}
    	List<ECPoint> Ps = new ArrayList<ECPoint>(pubKeys.size());
    	List<ECPoint> Rs = new ArrayList<ECPoint>(RBytes.size());
    	for(byte[] pubKey : pubKeys){
    		Ps.add(decodePoint(pubKey));
    	}
    	for(byte[] R : RBytes){
    		Rs.add(decodePoint(R));
    	}
    	return verifyMulSig(datas, Ps, Rs, sumOfS);
    }
	
    private static boolean verifyMulSig(List<byte[]> datas, List<SchnorrSignature> signatures, List<ECPoint> Ps) {
    	BigInteger sumOfS = BigInteger.ONE;
    	List<ECPoint> Rs = new ArrayList<ECPoint>(signatures.size());
    	for(SchnorrSignature sign : signatures){
    		sumOfS.add(sign.getS()).mod(order);
    		Rs.add(sign.getR());
    	}
		return verifyMulSig(datas, Ps, Rs, sumOfS);
	}
    
    public static boolean verify(List<byte[]> datas, List<byte[]> signs, List<byte[]> pubKeys) {
    	int size = datas.size();
    	if (size != signs.size() || size != pubKeys.size()) {
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
//			return Utils.bigIntegerToBytes(R.getAffineXCoord().toBigInteger(), 32);
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
	}
	
}
