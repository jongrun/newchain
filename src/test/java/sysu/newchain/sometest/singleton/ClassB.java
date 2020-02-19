package sysu.newchain.sometest.singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassB {
	private static final Logger logger = LoggerFactory.getLogger(ClassB.class);
	
	
	private static ClassB CLASS_B;
	
	private static class Holder{
		private static final ClassB CLASS_B = new ClassB();
	}
	
	private ClassA classA;
	
	static{
		logger.debug("class B static block");
	}
	
	private ClassB() {
		classA = ClassA.getInstance();
		logger.debug("ClassB()");
	}
	
	public static ClassB getInstance() {
		logger.debug("CLASS_B == null: {}", Holder.CLASS_B == null);
		return Holder.CLASS_B;
	}
	
//	public static ClassB getInstance() {
//		if (CLASS_B == null) {
//			CLASS_B = new ClassB();
//		}
//		return CLASS_B;
//	}

}
