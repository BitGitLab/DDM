package test.examples.manager;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestManager {

    @Test
    public void test() {
        try {
            String[] args = {};
            Manager.main(args);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
