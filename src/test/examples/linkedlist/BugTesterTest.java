package test.examples.linkedlist;

import static org.junit.Assert.*;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)

public class BugTesterTest {

	@Test
	public void test() {
		try {
			BugTester.main(null);
		} catch (Exception e) {
			System.out.println("error detected");
//			fail();
		}
	}

}
