package sysu.newchain.common.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

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
		logger.debug("r: {}", r.longValue());
		// R = rG
        ECPoint R = G.multiply(r).normalize();
        logger.debug("R: {}", R.getAffineXCoord().toBigInteger());
        
        /* Checks if R is a quadratic residue (?) */
        while (jacobi(R.getAffineYCoord().toBigInteger()) != 1) {
            r = (order.subtract(r));
            R = G.multiply(r).normalize();
        }
        
        // e = hash(R, P, m)
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(R.getEncoded(true), getPubKeyAsBytes(), input))).mod(order);
        
        // s = r + ke
        BigInteger s = r.add(e.multiply(priKey)).mod(order);
        logger.debug("s: {}, r: {}, k: {}, e: {}", s, r, priKey, e);
        logger.debug("s: {}, r: {}, k: {}, e: {}", s.longValue(), r.longValue(), priKey.longValue(), e.longValue());
        
        return new SchnorrSignature(R, s);
	}
	
    private static int jacobi(BigInteger x) {
        return IntegerFunctions.jacobi(x, p);
    }
	
    public static boolean verify(byte[] data, SchnorrSignature signature, byte[] pubKey) {
        if (!(signature.getR().getAffineXCoord().toBigInteger().compareTo(p) == -1)) {
            logger.debug("Failed cuz Rx greater than curve modulus");
            return false;
        }

        if (!(signature.getS().compareTo(order) == -1)) {
        	logger.debug("Failed cuz s greater than curve order");
            return false;
        }
        
        ECPoint P = decodePoint(pubKey);
        
        if (!ecSpec.getCurve().importPoint(P).isValid()) {
        	logger.debug("Failed cuz invalid point");
            return false;
        }
    	
        /* s = r + ke
         * sG = (r + ke)G = rG + Pe
         */
        BigInteger e = Utils.bytesToBigInteger(Hash.SHA256.hash(Utils.merge(signature.getRBytes(), pubKey, data))).mod(order);
        
        ECPoint S1 = G.multiply(signature.getS()).normalize();
        ECPoint S2 = signature.getR().add(P.multiply(e).normalize()).normalize();
        
        if (!(S1.getAffineXCoord().toBigInteger().compareTo(S2.getAffineXCoord().toBigInteger()) == 0)) {
        	logger.debug("S1 != S2");
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
