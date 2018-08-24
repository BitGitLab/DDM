package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.test.Test120;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testPool1 {

    @Test
    public void test(){

        try {

            Test120.main(null);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
