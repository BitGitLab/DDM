package test.examples.wrongLock;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestwrongLock {

    @Test
    public void test(){

        try {

            String[] args = {};
            Main.main(args);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
