package sysu.newchain;

import sysu.newchain.service.Service;

public class AppOnlyServer {
	
	public static void main(String[] args) throws Exception {
		Service service = Service.getInstance();
		service.init();
		service.start();
	}
}
