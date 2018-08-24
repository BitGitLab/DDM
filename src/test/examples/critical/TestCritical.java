package test.examples.critical;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestCritical {

    @Test
    public void Test(){

        try {

            Critical.main(null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
