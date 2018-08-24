package test.examples.pingpong;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class Testpingpong {

    @Test
    public void test(){

        try {

            PingPong.main(null);
        }catch (Exception e){

            e.printStackTrace();
        }
    }
}
