package sysu.newchain.sometest;

public class ThreeNum {
	public static void main(String[] args) {
		int a = 1, b = 1, c = 1;
		int temp;
		for (int i = 0; i < 20190324 - 3; i++) {
			temp = (a + b + c) % 10000;
			a = b;
			b = c;
			c = temp;
		}
		System.out.println(c);
		
		int[] nums = {1, 1, 1};
		int index = -1;
		for (int i = 0; i < 20190324 - 3; i++) {
			index = (index + 1) % 3;
			nums[index] = (nums[0] + nums[1] + nums[2]) % 10000;
		}
		System.out.println(nums[index]);
	}
}
