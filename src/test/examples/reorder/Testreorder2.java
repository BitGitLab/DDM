package test.examples.reorder;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class Testreorder2 {

    @Test
    public void test(){

        try {

            ReorderTest2.main(null);
        }catch (Exception e){

            e.printStackTrace();
        }
    }
}
