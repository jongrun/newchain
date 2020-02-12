package sysu.newchain.properties;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;


/**
 * @Description 用于读取节点信息
 * @author jongliao
 * @date 2020年1月22日 下午4:56:31
 */
public class NodesProperties {
	
	private static NodeContainer nodeContainer;
	
	private NodesProperties() {}
	
	static {
		String filePath = NodesProperties.class.getResource("/").getPath() + File.separator + AppConfig.getNodesFile();
//		System.out.println(filePath);
		File file = new File(filePath);
		ObjectMapper mapper = new JavaPropsMapper();
		try {
			nodeContainer = mapper.readValue(file, NodeContainer.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static NodeInfo get(long nodeId) throws JsonParseException, JsonMappingException, IOException {
		for (NodeInfo nodeInfo : nodeContainer.node) {
			if (nodeInfo.getId() == nodeId) {
				return nodeInfo;
			}
		}
		return null;
	}
	
	public static int getNodesSize() {
		return nodeContainer.node.size();
	}
	
	static class NodeContainer{
		public List<NodeInfo> node;
	}
}
