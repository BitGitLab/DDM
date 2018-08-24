package test.examples.store;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class Teststore {

    @Test
    public void test(){

        try{

            StoreTest.main(null);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
