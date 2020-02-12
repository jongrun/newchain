package sysu.newchain.common.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.swing.text.Utilities;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.x9.DomainParameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.FixedPointUtil;
import org.bouncycastle.util.encoders.Hex;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.io.BaseEncoding;

import sysu.newchain.common.format.Base58;
import sysu.newchain.core.Address;

/**
 * @Description 椭圆曲线算法的私钥、公钥，用于签名
 * @author jongliao
 * @date 2020年1月20日 上午10:24:30
 */
public class ECKey {
//	public static final Logger logger = LogManager.getLogger(ECKey.class);
	public static final Logger logger = LoggerFactory.getLogger(ECKey.class);
	
	BigInteger priKey;
	ECPoint pubKey;
	
	public static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
	public static final ECDomainParameters CURVE;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
	/**
     * Equal to CURVE.getN().shiftRight(1), used for canonicalising the S value of a signature. If you aren't
     * sure what this is about, you can ignore it.
     */
    public static final BigInteger HALF_CURVE_ORDER;
    
	static {
		FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
		CURVE = new ECDomainParameters(
				CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), 
				CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
		HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);
	}
	
	public ECKey() {
		ECKeyPairGenerator generator = new ECKeyPairGenerator();
		ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(CURVE, SECURE_RANDOM);
		generator.init(keyGenerationParameters);
		AsymmetricCipherKeyPair keyPair2 = generator.generateKeyPair();
		ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keyPair2.getPrivate();
		ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keyPair2.getPublic();
		priKey = privParams.getD();
		pubKey = pubParams.getQ();
	}
	
	public ECKey(BigInteger priKey, ECPoint pubKey) {
		this.priKey = priKey;
		this.pubKey = pubKey;
	}
	
	public ECPoint getPubKey() {
		return pubKey;
	}
	
	public byte[] getPubKeyAsBytes() {
		return pubKey.getEncoded(true);
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
			return bigIntegerToBytes(priKey, 32);
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
	
	private static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
		byte[] src = b.toByteArray();
		byte[] dest = new byte[numBytes];
		boolean isFirstByteOnlyForSign = src[0] == 0;
		int length = isFirstByteOnlyForSign ? src.length - 1 : src.length;
		int srcPos = isFirstByteOnlyForSign ? 1 : 0;
		int destPos = numBytes - length;
		System.arraycopy(src, srcPos, dest, destPos, length);
		return dest;
	}
	
	public static ECKey fromPrivate(BigInteger priKey) {
		ECPoint pubKey = new FixedPointCombMultiplier().multiply(CURVE.getG(), priKey);
		return new ECKey(priKey, pubKey);
	}
	
	public static ECKey fromPrivate(byte[] priKeyBytes) {
		return fromPrivate(new BigInteger(1, priKeyBytes));
	}
	
	public static ECKey fromPubKeyOnly(ECPoint pubKey) {
		return new ECKey(null, pubKey);
	}
	
	public static ECKey fromPubKeyOnly(byte[] pubKey) {
		return new ECKey(null, CURVE.getCurve().decodePoint(pubKey));
	}
	
	public Address toAddress() throws Exception {
		return new Address(getPubKeyHash());
	}
	
	public ECDSASignature sign(byte[] input) throws Exception {
		if (priKey == null) {
			throw new Exception("Missing private key!");
		}
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(priKey, CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(input);
        return new ECDSASignature(components[0], components[1]).toCanonicalised();
	}
	
    /**
     * Verifies the given ECDSA signature against the message bytes using the public key bytes.</p>
     * When using native ECDSA verification, data must be 32 bytes, and no element may be
     * larger than 520 bytes.</p>
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @param pub       The public key bytes to use.
     * @return 是否验签成功
     */
    public static boolean verify(byte[] data, ECDSASignature signature, byte[] pub) {
        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        try {
            boolean ret = signer.verifySignature(data, signature.r, signature.s);
            return ret;
        } catch (NullPointerException e) {
            // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those signatures
            // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
            logger.error("Caught NPE inside bouncy castle", e);
           return false;
        }
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @param pub       The public key bytes to use.
     * @return
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return verify(data, ECDSASignature.decodeFromDER(signature), pub);
    }
    
    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     * @return
     */
    public boolean verify(byte[] data, ECDSASignature signature) {
    	return ECKey.verify(data, signature, getPubKeyAsBytes());
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param data      Hash of the data to verify.
     * @param signature ASN.1 encoded signature.
     */
    public boolean verify(byte[] data, byte[] signature) {
        return ECKey.verify(data, signature, getPubKeyAsBytes());
    }
    
	public static class ECDSASignature{
		public final BigInteger r, s;

		public ECDSASignature(BigInteger r, BigInteger s) {
			super();
			this.r = r;
			this.s = s;
		}
		
        /**
         * DER is an international standard for serializing data structures which is widely used in cryptography.
         * It's somewhat like protocol buffers but less convenient. This method returns a standard DER encoding
         * of the signature, as recognized by OpenSSL and other libraries.
         * @return a standard DER encoding of the signature
         */
        public byte[] encodeToDER() {
            try {
                return derByteStream().toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);  // Cannot happen.
            }
        }

        public static ECDSASignature decodeFromDER(byte[] bytes) throws IllegalArgumentException {
            ASN1InputStream decoder = null;
            try {
                decoder = new ASN1InputStream(bytes);
                final ASN1Primitive seqObj = decoder.readObject();
                if (seqObj == null)
                    throw new IllegalArgumentException("Reached past end of ASN.1 stream.");
                if (!(seqObj instanceof DLSequence))
                    throw new IllegalArgumentException("Read unexpected class: " + seqObj.getClass().getName());
                final DLSequence seq = (DLSequence) seqObj;
                ASN1Integer r, s;
                try {
                    r = (ASN1Integer) seq.getObjectAt(0);
                    s = (ASN1Integer) seq.getObjectAt(1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(e);
                }
                // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } finally {
                if (decoder != null)
                    try { decoder.close(); } catch (IOException x) {}
            }
        }

        public ByteArrayOutputStream derByteStream() throws IOException {
            // Usually 70-72 bytes.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(72);
            DERSequenceGenerator seq = new DERSequenceGenerator(bos);
            seq.addObject(new ASN1Integer(r));
            seq.addObject(new ASN1Integer(s));
            seq.close();
            return bos;
        }

        /**
         * Returns true if the S component is "low", that means it is below {@link ECKey#HALF_CURVE_ORDER}. See <a
         * href="https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures">BIP62</a>.
         */
        public boolean isCanonical() {
            return s.compareTo(HALF_CURVE_ORDER) <= 0;
        }

        /**
         * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
         * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
         * the same message. However, we dislike the ability to modify the bits of a Bitcoin transaction after it's
         * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
         * considered legal and the other will be banned.
         * @return
         */
        public ECDSASignature toCanonicalised() {
            if (!isCanonical()) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                return new ECDSASignature(r, CURVE.getN().subtract(s));
            } else {
                return this;
            }
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ECDSASignature other = (ECDSASignature) o;
            return r.equals(other.r) && s.equals(other.s);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(r, s);
        }
    }
	
	public static void main(String[] args) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		keyPairGenerator.initialize(256);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		
		ECPublicKey ecPublicKey = (ECPublicKey) keyPair.getPublic();
		ECPrivateKey ecPrivateKey = (ECPrivateKey) keyPair.getPrivate();
		
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(ecPrivateKey.getEncoded());
		System.out.println(new String(Hex.encode(pkcs8EncodedKeySpec.getEncoded())));
		
		KeyFactory keyFactory = KeyFactory.getInstance("EC");
		keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		
		
		System.out.println(ecPublicKey.toString());
		System.out.println(ecPrivateKey.toString());
		System.out.println(ecPublicKey.getW().getAffineX().toString(16));
		System.out.println(ecPublicKey.getW().getAffineY().toString(16));
		System.out.println(ecPrivateKey.getS().toString(16));
		System.out.println(BaseEncoding.base16().encode(ecPublicKey.getEncoded()) + " size: " + ecPublicKey.getEncoded().length);
		System.out.println(BaseEncoding.base16().encode(ecPrivateKey.getEncoded()) + " size: " + ecPrivateKey.getEncoded().length);
		
		ECKeyPairGenerator generator = new ECKeyPairGenerator();
		ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(CURVE, SECURE_RANDOM);
		generator.init(keyGenerationParameters);
		AsymmetricCipherKeyPair keyPair2 = generator.generateKeyPair();
		ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keyPair2.getPrivate();
		ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keyPair2.getPublic();
		BigInteger priv = privParams.getD();
		ECPoint pub = pubParams.getQ();
		System.out.println(new String(Hex.encode(bigIntegerToBytes(priv, 32))));
		System.out.println(new String(Hex.encode(pub.getEncoded(true))));
		logger.debug("debug test");
		logger.error("error test");
	}
}