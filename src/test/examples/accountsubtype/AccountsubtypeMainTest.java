package test.examples.accountsubtype;

import static org.junit.Assert.*;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JUnit4MCRRunner.class)
public class AccountsubtypeMainTest {

	@Test
	public void test() {
		
		try {
			Main.main(null);
		} catch (Exception e) {
			System.out.println("error detected");
//			fail();
		}
	}

}
