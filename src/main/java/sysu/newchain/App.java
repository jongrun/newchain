package sysu.newchain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import sysu.newchain.consensus.server.RequestResponer;
import sysu.newchain.consensus.server.service.ConsensusService;
import sysu.newchain.service.Service;

@SpringBootApplication(scanBasePackages = "sysu.newchain")
public class App implements CommandLineRunner{
	Service service = Service.getInstance();
	
	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		service.init();
		service.start();
	}
}