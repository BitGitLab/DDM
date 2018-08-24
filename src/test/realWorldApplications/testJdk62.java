package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import test.Test4742723;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testJdk62 {

    @Test
    public void test(){

        try {

            String[] args = {};
            Test4742723.main(args);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
