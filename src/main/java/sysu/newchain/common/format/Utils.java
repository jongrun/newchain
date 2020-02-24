package sysu.newchain.common.format;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;

public class Utils {

	/** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in little endian format. */
    public static long readUint32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
                ((bytes[offset + 1] & 0xffl) << 8) |
                ((bytes[offset + 2] & 0xffl) << 16) |
                ((bytes[offset + 3] & 0xffl) << 24);
    }

    /** Parse 8 bytes from the byte array (starting at the offset) as signed 64-bit integer in little endian format. */
    public static long readInt64(byte[] bytes, int offset) {
        return (bytes[offset] & 0xffl) |
               ((bytes[offset + 1] & 0xffl) << 8) |
               ((bytes[offset + 2] & 0xffl) << 16) |
               ((bytes[offset + 3] & 0xffl) << 24) |
               ((bytes[offset + 4] & 0xffl) << 32) |
               ((bytes[offset + 5] & 0xffl) << 40) |
               ((bytes[offset + 6] & 0xffl) << 48) |
               ((bytes[offset + 7] & 0xffl) << 56);
    }

    /** Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big endian format. */
    public static long readUint32BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xffl) << 24) |
                ((bytes[offset + 1] & 0xffl) << 16) |
                ((bytes[offset + 2] & 0xffl) << 8) |
                (bytes[offset + 3] & 0xffl);
    }

    /** Parse 2 bytes from the byte array (starting at the offset) as unsigned 16-bit integer in big endian format. */
    public static int readUint16BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xff) << 8) |
                (bytes[offset + 1] & 0xff);
    }
    
    public static void uint16ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & val));
        stream.write((int) (0xFF & (val >> 8)));
    }
    
    public static void uint32ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & val));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
    }
    
    public static void int64ToByteStreamLE(long val, OutputStream stream) throws IOException {
        stream.write((int) (0xFF & val));
        stream.write((int) (0xFF & (val >> 8)));
        stream.write((int) (0xFF & (val >> 16)));
        stream.write((int) (0xFF & (val >> 24)));
        stream.write((int) (0xFF & (val >> 32)));
        stream.write((int) (0xFF & (val >> 40)));
        stream.write((int) (0xFF & (val >> 48)));
        stream.write((int) (0xFF & (val >> 56)));
    }

    public static void uint64ToByteStreamLE(BigInteger val, OutputStream stream) throws IOException {
        byte[] bytes = val.toByteArray();
        if (bytes.length > 8) {
            throw new RuntimeException("Input too large to encode into a uint64");
        }
        bytes = reverseBytes(bytes);
        stream.write(bytes);
        if (bytes.length < 8) {
            for (int i = 0; i < 8 - bytes.length; i++)
                stream.write(0);
        }
    }
    
    public static void uint32ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }
    
    public static void uint64ToByteArrayLE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }
    
    public static void uint32ToByteArrayBE(long val, byte[] out, int offset) {
        out[offset] = (byte) (0xFF & (val >> 24));
        out[offset + 1] = (byte) (0xFF & (val >> 16));
        out[offset + 2] = (byte) (0xFF & (val >> 8));
        out[offset + 3] = (byte) (0xFF & val);
    }
    
	public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
		if (b == null) {
			return null;
		}
		byte[] src = b.toByteArray();
		byte[] dest = new byte[numBytes];
		boolean isFirstByteOnlyForSign = src[0] == 0;
		int length = isFirstByteOnlyForSign ? src.length - 1 : src.length;
		int srcPos = isFirstByteOnlyForSign ? 1 : 0;
		int destPos = numBytes - length;
		System.arraycopy(src, srcPos, dest, destPos, length);
		return dest;
	}
	
    /**
     * Cast hex encoded value from byte[] to BigInteger
     * null is parsed like byte[0]
     *
     * @param bb byte array contains the values
     * @return unsigned positive BigInteger value.
     */
    public static BigInteger bytesToBigInteger(byte[] bb) {
        return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
    }
	
	public static byte[] longToBytes(long l) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(l);
	    return buffer.array();
	}
	
	public static long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip();
	    return buffer.getLong();
	}
	
    public static byte[] reverseBytes(byte[] bytes) {
        // We could use the XOR trick here but it's easier to understand if we don't. If we find this is really a
        // performance issue the matter can be revisited.
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }
	
    public static byte[] copyOf(byte[] in, int length) {
        byte[] out = new byte[length];
        System.arraycopy(in, 0, out, 0, Math.min(length, in.length));
        return out;
    }
    
    /**
     * @param arrays - arrays to merge
     * @return - merged array
     */
    public static byte[] merge(byte[]... arrays)
    {
        int count = 0;
        for (byte[] array: arrays)
        {
            count += array.length;
        }
        
        // Create new array and copy all array contents
        byte[] mergedArray = new byte[count];
        int start = 0;
        for (byte[] array: arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }
    
    public static String toString(byte[] bytes, String charsetName) {
        try {
            return new String(bytes, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
