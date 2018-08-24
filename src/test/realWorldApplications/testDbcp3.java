package test.realWorldApplications;

import controller.exploration.JUnit4MCRRunner;
import org.apache.commons.dbcp.datasources.Dbcp369;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

@RunWith(JUnit4MCRRunner.class)
public class testDbcp3 {

    @Test
    public void test(){

        try {

            String[] args = {};
            Dbcp369.main(args);
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
}
