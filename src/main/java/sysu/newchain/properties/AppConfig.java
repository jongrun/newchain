package sysu.newchain.properties;

import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class AppConfig {
	
	// ∂¡»°≈‰÷√Œƒº˛
	static ResourceBundle resourceBundle = ResourceBundle.getBundle(System.getProperty("config", "application"));
	
	static String nodesFile = get("nodesFile", "node.properties");
	
	static long nodeId = Long.parseLong(get("nodeId"));
	
	static String nodePriKey = get("nodePriKey");
	
	static String dataDir = get("dataDir", "data");
	
	static String clientPriKey = get("clientPriKey");
	
	static int blockBuildInternal = Integer.parseInt(get("blockBuildInternal", "500"));
	
	static int requestTimeout = Integer.parseInt(get("requestTimeout", "10"));
	
	static boolean isMulSig = Boolean.parseBoolean(get("isMulSig"));
	
	public static String getNodesFile() {
		return nodesFile;
	}

	public static long getNodeId() {
		return nodeId;
	}

	public static String getNodePriKey() {
		return nodePriKey;
	}
	
	public static String getDataDir() {
		return dataDir;
	}
	
	public static String getClientPriKey() {
		return clientPriKey;
	}

	public static int getBlockBuildInternal() {
		return blockBuildInternal;
	}
	
	public static int getRequestTimeout() {
		return requestTimeout;
	}
	
	public static String get(String key) {
		return get(key, null);
	}
	
	public static String get(String key, String def) {
		try {
			return resourceBundle.getString(key);
		} catch (Exception e) {
			return def;
		}
	}
	
	public static void main(String[] args) {
		if (get("test") == null) {
			System.out.println("null");
		}
		else {
			System.out.println(get("test"));
		}
	}
	
	public static boolean isMulSig() {
		return isMulSig;
	}
}
