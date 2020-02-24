package sysu.newchain.tools;

import java.io.IOException;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import sysu.newchain.common.crypto.SchnorrKey;
import sysu.newchain.common.format.Base58;
import sysu.newchain.common.format.Hex;
import sysu.newchain.core.Address;
import sysu.newchain.core.Transaction;

public class GenTransaction {
	private static Random random = new Random();
	private static Logger logger = LoggerFactory.getLogger(GenTransaction.class);
	public static Transaction genTransaction() throws Exception {
		SchnorrKey ecKey = SchnorrKey.fromPrivate(Base58.decode("FcbyAoZztZMPuaGWMfTy4Hduhz5aFHooSfqD4QyKtqUq"));
		Transaction transaction = new Transaction(
				new Address("18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD"), 
				new Address("1K6RU9MY9nX8R6SFUN56rKrHGZWwk7tvcF"), 
				20, 
				System.currentTimeMillis() + "" + random.nextInt(), 
				null,
				ecKey.getPubKeyAsBytes(), 
				"good luck".getBytes());
//		logger.debug("tx time: " + transaction.getTime());
//		transaction.setSign(ecKey.sign(transaction.calculateAndSetHash()).encodeToDER());
		transaction.calculateAndSetSign(ecKey);
		return transaction;
	}
	
	public static void main(String[] args) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree("{\r\n" + 
				"    \"jsonrpc\": \"2.0\",\r\n" + 
				"    \"method\": \"insertTransaction\",\r\n" + 
				"    \"params\": {\r\n" + 
				"        \"from\": \"18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD\",\r\n" + 
				"        \"to\": \"18v3rD1xWoeXy6yiHCe5e4LhorSXhZg8GD\",\r\n" + 
				"        \"amount\": 20,\r\n" + 
				"        \"time\": \"123334\",\r\n" + 
				"        \"sign\": \"xixix\",\r\n" + 
				"        \"pubKey\": \"eS9avE7g642UygBiudaLWzYcReFHQurDZ8z5kznUFFER\",\r\n" + 
				"        \"data\": \"hahahaah\"\r\n" + 
				"    },\r\n" + 
				"    \"id\": 1\r\n" + 
				"}");
		Transaction transaction = genTransaction();
		ObjectNode params = (ObjectNode) node.get("params");
		params.set("from", new TextNode(transaction.getFrom().getEncodedBase58()));
		params.set("to", new TextNode(transaction.getTo().getEncodedBase58()));
		params.set("amount", new LongNode(transaction.getAmount()));
		params.set("time", new TextNode(transaction.getTime()));
		params.set("sign", new TextNode(Base58.encode(transaction.getSign())));
		params.set("pubKey", new TextNode(Base58.encode(transaction.getPubKey())));
		params.set("data", new TextNode(Base58.encode(transaction.getData())));
		System.out.println(node.toString());
	}
}
