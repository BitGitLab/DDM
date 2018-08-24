package test.examples.checkfield;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestCheckField {

    @Test
    public void test() {
        try {

            CheckField.main(null);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
