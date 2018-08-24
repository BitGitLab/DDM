package test.examples.buggyprogram;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestBuggyProgram {

    @Test
    public void test() {
        try {

            String[] args = {};
            BuggyProgram.main(args);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
