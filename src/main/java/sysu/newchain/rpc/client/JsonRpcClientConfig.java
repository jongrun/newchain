package sysu.newchain.rpc.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sysu.newchain.rpc.api.ChainAPI;
import sysu.newchain.rpc.api.impl.ChainAPIImpl;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;

@Configuration
public class JsonRpcClientConfig {
	private static final String endpoint = "http://localhost:8080/newchain";
	
	@Bean
	public JsonRpcHttpClient jsonRpcHttpClient() throws MalformedURLException {
		URL url = null;
		Map<String, String> map = new HashMap<String, String>();
		url = new URL(endpoint);
		return new JsonRpcHttpClient(url, map);
	}
	
	@Bean
	public ChainAPI chainAPI(JsonRpcHttpClient jsonRpcHttpClient){
		return ProxyUtil.createClientProxy(getClass().getClassLoader(), ChainAPI.class, jsonRpcHttpClient);
	}
}
