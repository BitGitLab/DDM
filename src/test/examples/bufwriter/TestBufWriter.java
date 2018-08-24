package test.examples.bufwriter;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestBufWriter {

    @Test
    public void test() {
        try {

            String[] args = {};
            BufWriter.main(args);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
