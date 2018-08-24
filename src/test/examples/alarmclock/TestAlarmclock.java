package test.examples.alarmclock;


import controller.exploration.JUnit4MCRRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnit4MCRRunner.class)
public class TestAlarmclock {

    @Test
    public void test() {
        try {

            AlarmClock.main(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
