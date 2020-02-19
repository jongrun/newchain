package sysu.newchain.sometest.singleton;

import sysu.newchain.consensus.BlockBuildManager;

public class SingletonTest {
	ClassA classA = ClassA.getInstance();
	ClassB classB = ClassB.getInstance();
	public static void main(String[] args) {		
		SingletonTest singletonTest = new SingletonTest();
	}
}
