package sysu.newchain.client;

public class ClientTest {
	public static void main(String[] args) throws Exception {
		Client client = new Client("client-1");
		Client client2 = new Client("client-2");
		client.start();
		client2.start();
	}
}
