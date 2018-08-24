package test.examples.airlinetickets;

import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestAirlineTickets {

    @Test
    public void test() {
        try {

            String[] args = {};
            Airlinetickets.main(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("error detected");

        }
    }
}
