package test.examples.reorder;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class Testreorder {

    @Test
    public void test(){

        try {

            ReorderTest.main(null);
        }catch (Exception e){

            e.printStackTrace();
        }
    }
}
