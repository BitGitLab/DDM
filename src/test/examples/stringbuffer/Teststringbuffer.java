package test.examples.stringbuffer;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class Teststringbuffer {

    @Test
    public void test(){

        try {

            StringBufferTest.main(null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
