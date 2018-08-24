package test.examples.consisitency;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestMain{

	@Test
	public void Test(){

		try {
			Main.main(null);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
