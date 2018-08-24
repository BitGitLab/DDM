package test.examples.allocationvector;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.fail;

@RunWith(JUnit4MCRRunner.class)
public class TestAllocation {
    @Test
    public void test() {
        try{
            String[] args = new String[]{};
            AllocationTest.main(args);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
