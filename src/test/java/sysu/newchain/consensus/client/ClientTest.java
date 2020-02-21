package sysu.newchain.consensus.client;

import sysu.newchain.consensus.client.RequestClient;

public class ClientTest {
	public static void main(String[] args) throws Exception {
		RequestClient client = new RequestClient("client-1");
		RequestClient client2 = new RequestClient("client-2");
		client.start();
		client2.start();
	}
}
