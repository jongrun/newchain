package sysu.newchain.core;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;

import sysu.newchain.common.crypto.Hash;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Serialize;

public class Address{
	
	private int version = 0x00;
	private byte[] addressBytes;  // 20 bytes
	private String encodedBase58;
	
	public Address(byte[] addressBytes) throws Exception{
		this.addressBytes = addressBytes;
		this.encodedBase58 = toVersionedChecksummedBase58();
	}
	
	public Address(int version, byte[] addressBytes) throws Exception {
		this(addressBytes);
		this.version = version;
	}
	
	public Address(String encodedBase58) throws Exception {
		byte[] decoded = Base58.decode(encodedBase58);
		if (decoded.length < 4) {
			throw new Exception("Address bytes is shorter than 4 bytes!");
		}
		byte[] versionedBytes = Arrays.copyOf(decoded, decoded.length - 4);
		byte[] checksum = Arrays.copyOfRange(decoded, decoded.length - 4, decoded.length);
		if (!Arrays.equals(checksum, Arrays.copyOf(Hash.SHA256.hashTwice(versionedBytes), 4))) {
			throw new Exception("Checksum in address is not valid!");
		}
		this.version = versionedBytes[0] & 0xFF;
		this.addressBytes = new byte[versionedBytes.length - 1];
		System.arraycopy(versionedBytes, 1, this.addressBytes, 0, versionedBytes.length - 1);
		this.encodedBase58 = encodedBase58;
	}
	
	public int getVersion() {
		return version;
	}
	
	public byte[] getAddressBytes() {
		return addressBytes;
	}

	public String getEncodedBase58() {
		return encodedBase58;
	}

	private String toVersionedChecksummedBase58() {
		int addressBytesLen = this.addressBytes.length;
		byte[] versionedChecksummedBytes = new byte[1 + addressBytesLen + 4];
		versionedChecksummedBytes[0] = (byte) this.version;
		System.arraycopy(this.addressBytes, 0, versionedChecksummedBytes, 1, addressBytesLen);
		byte[] checksum = Hash.SHA256.hashTwice(versionedChecksummedBytes, 0, addressBytesLen + 1);
		System.arraycopy(checksum, 0, versionedChecksummedBytes, addressBytesLen + 1, 4);
		return Base58.encode(versionedChecksummedBytes);
	}
	
	public static Address deserialize(byte[] buff) {
		try {
			return new Address(Base58.encode(buff));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
