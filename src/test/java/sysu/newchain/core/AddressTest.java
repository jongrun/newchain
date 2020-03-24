package sysu.newchain.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.jgroups.util.ByteArrayDataOutputStream;

import sysu.newchain.common.core.Address;
import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Hex;
import sysu.newchain.common.format.Serialize;

public class AddressTest {
	public static void main(String[] args) throws Exception {
		SchnorrKey ecKey = SchnorrKey.fromPrivate(Hex.decode("d117a4779f4fa0ee3581f871c411b0e48258c22a54e9f09d5d92e168a69abddf"));
		System.out.println(ecKey.getPubKeyAsHex().equals("025f88e726c4c9957522cf949ab53cfd79810890e7cc7599ead3c9d14864d88ec1"));
		System.out.println(Hex.encode(ecKey.getPubKeyHash()).equals("03034e76c6b9a8c1a77f027bde58ea3700f5c311"));
		Address address = ecKey.toAddress();
		System.out.println(address.getEncodedBase58().equals("1GvzKH6myTG8biSh57UEPwDDS6m4rYRjp"));
		
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(buff);
		out.writeObject(address);
		System.out.println(Hex.encode(buff.toByteArray()));
		System.out.println(address.getVersion());
		
		
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(buff.toByteArray()));
		Address address3 = (Address) in.readObject();
		System.out.println(address3.getEncodedBase58());
	}
}
