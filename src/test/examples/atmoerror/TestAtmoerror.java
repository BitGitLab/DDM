package test.examples.atmoerror;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestAtmoerror {

    @Test
    public void test() {
        try {
            Main.main(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
