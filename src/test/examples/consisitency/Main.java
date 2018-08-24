package test.examples.consisitency;

public class Main implements Runnable{
	public static int THREAD_NUMBER = 3;
	
	public static int a = 0;
	public static int b = 0;
	private int num;
	
	public Main(int num) {
		this.num = num;
	}
	
	public void run() {
		a = num;
		b = num;
	}

	public static void main(String[] args) throws Exception {

		a = 0;
		b = 0;
		Thread[] t = new Thread[THREAD_NUMBER];
		for (int i = 0; i < THREAD_NUMBER; i++) {
			t[i] = new Thread(new Main(i));
			t[i].start();
		}

		for (int i = 0; i < THREAD_NUMBER; i++) {
			t[i].join();
		}

		System.out.println("a = " + a + ", b = " + b);
		assert a==b : ("bug found.");
	}
}
