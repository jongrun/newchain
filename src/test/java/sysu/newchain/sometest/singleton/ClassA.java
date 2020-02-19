package sysu.newchain.sometest.singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassA {
	private static final Logger logger = LoggerFactory.getLogger(ClassA.class);
	
	private static class Holder {
		private static final ClassA CLASS_A = new ClassA();
		
	}
	
	private static ClassA CLASS_A;
	
	private ClassB classB;
	
	static{
		logger.debug("class A static block");
	}
	
	private ClassA(){
		classB = ClassB.getInstance();
		logger.debug("ClassA()");
	}
	
	public static ClassA getInstance() {
		logger.debug("CLASS_A == null: {}", Holder.CLASS_A == null);
		return Holder.CLASS_A;
	}
	
//	public static ClassA getInstance() {
//		logger.debug("CLASS_A == null: {}", CLASS_A == null);
//		if (CLASS_A == null) {
//			CLASS_A = new ClassA();
//		}
//		return CLASS_A;
//	}
}