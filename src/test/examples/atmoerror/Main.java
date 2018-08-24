package test.examples.atmoerror;


public class Main {

	public static void main(String[] args) {

	      BankAccount account = new BankAccount();
	     
	      Thread t1 = new Thread(new Customer(5, account));
	      Thread t2 = new Thread(new Customer(5, account));
	      
	      t1.start();
	      t2.start();
	  }
}
