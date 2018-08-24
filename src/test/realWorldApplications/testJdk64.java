package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import test.Test4813150;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testJdk64 {

    @Test
    public void test(){

        try {

            String[] args = {};
            Test4813150.main(args);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
