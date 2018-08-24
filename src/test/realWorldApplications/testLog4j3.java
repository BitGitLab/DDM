package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.apache.log4j.helpers.Test54325;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testLog4j3 {

    @Test
    public void test(){

        try {

            String[] args = {};
            Test54325.main(args);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
