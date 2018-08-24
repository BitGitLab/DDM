package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.test.Test44032;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testLog4j1 {

    @Test
    public void test(){

        try {

            String[] args = {};
            Test44032.main(args);
        }catch (Exception e){
            e.printStackTrace();
//            fail();
        }
    }
}
