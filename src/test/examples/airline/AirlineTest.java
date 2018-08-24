package test.examples.airline;

import controller.exploration.JUnit4MCRRunner;
import junit.framework.Assert;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class AirlineTest {
    
    @Test
	public void test() throws InterruptedException {
        try {
			Main.main(new String[0]);
		} catch (Exception e) {
			System.out.println("here");
		}
	}

}
