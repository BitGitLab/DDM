package controller.exploration;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;

import controller.MCRProperties;
import controller.scheduling.strategy.MCRStrategy;
import controller.scheduling.strategy.RandomStrategy;
import engine.ExploreSeedInterleavings;
import engine.trace.Trace;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import pattern.schedule.Schedule;

/**
 * MCR runner for JUnit4 tests.
 * 
 */
public class JUnit4MCRRunner extends BlockJUnit4ClassRunner {

    /**
     * Constants
     */
    private static final String DOT = ".";

    private static final String INVALID_SYNTAX_MESSAGE = "Ignoring schedule because of invalid syntax: name = %s value = %s .\nCaused by: %s";
    private static final String EXPECT_DEADLOCK_MSG = "Expecting deadlock!";
    
    private static int used;

    /**
     * Currently executing test method and notifier and schedule.
     */
    private FrameworkMethod currentTestMethod;
    private RunNotifier currentTestNotifier;
    
    private boolean isDeadlockExpected = false;
    public static HashSet<String> npes = new HashSet<String>();

    private static JUnit4WrappedRunNotifier wrappedNotifier;
    public static FrameworkMethod method;

    /**
     * programe terminated when the first error detected
     */
    public static boolean stopOnFirstError;



    public JUnit4MCRRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    //start from here
    //the first to be executed in this class
    //and after instrumentation
    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {

        this.used = 0;
        this.currentTestMethod = method;
        this.currentTestNotifier = notifier;
        
        Trace.appname = method.getMethod().getDeclaringClass().getName();

        exploreTest(method, notifier);
    }
    
    /**
     * called by runChild
     * @param method
     * @param notifier
     */
    private void exploreTest(FrameworkMethod method, RunNotifier notifier) {

        stopOnFirstError = true;
        String stopOnFirstErrorString = MCRProperties.getInstance().getProperty(MCRProperties.STOP_ON_FIRST_ERROR_KEY); //true

        if (stopOnFirstErrorString.equalsIgnoreCase("false")) {
            stopOnFirstError = false;
        }
        
        JUnit4MCRRunner.method = method;
        String name = getTestClass().getName() + DOT + method.getName();

        Scheduler.startingExploration(name);

        wrappedNotifier = new JUnit4WrappedRunNotifier(notifier);
        wrappedNotifier.testExplorationStarted();

        Thread explorationThread = getNewExplorationThread();
        explorationThread.start();              //start the exploration

        //after the state space exploration finishes
        while (true) {
            try {
                // wait for either a normal finish or a deadlock to occur
                Scheduler.getTerminationNotifer().acquire();
                while (explorationThread.getState().equals(Thread.State.RUNNABLE)) {
                    Thread.yield();
                }
                // check for deadlock
                if (!isDeadlockExpected && (explorationThread.getState().equals(Thread.State.WAITING) ||
                        explorationThread.getState().equals(Thread.State.BLOCKED))) {

//                    System.out.println("explorationThread.getState():" + explorationThread.getState());

                    Scheduler.failureDetected("Deadlock detected in schedule");
                    Scheduler.completedScheduleExecution(); //call  the mcr method
                    wrappedNotifier.fireTestFailure(new Failure(describeChild(method), new RuntimeException("Deadlock detected in schedule")));
                    wrappedNotifier.setFailure(null); // workaround to prevent
                                                      // exploration thread from
                                                      // thinking that a
                                                      // previous failure means
                                                      // a failure in current
                                                      // schedule
                    // if we should continue exploring from deadlock
                    if (!stopOnFirstError) {
//                        Scheduler.startingExploration(name);
//                        wrappedNotifier.testExplorationStarted();

                        // leave currently deadlocked threads in place
                        explorationThread = getNewExplorationThread();
                        explorationThread.start();
                        //stopOnFirstError = true;
                        continue;
                    }
                } else if (isDeadlockExpected && (explorationThread.getState().equals(Thread.State.WAITING) ||
                        explorationThread.getState().equals(Thread.State.BLOCKED))) {
                    Scheduler.completedScheduleExecution();
                    wrappedNotifier.setFailure(null);
                    explorationThread = getNewExplorationThread();
                    explorationThread.start();
                    continue;
                }
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(2);
            }
        }   //end while
        wrappedNotifier.testExplorationFinished();
        Scheduler.completedExploration();
        
        System.err.println("memory used: " + ExploreSeedInterleavings.memUsed + "bytes.");
    }
    
    /**
     * called by exploreTest in this class
     * @return a thread
     */
    private Thread getNewExplorationThread() {
        
        return new Thread() {
            public void run() {               
                while (Scheduler.canExecuteMoreSchedules()) {

                    Scheduler.startingScheduleExecution();

                    JUnit4MCRRunner.super.runChild(method, wrappedNotifier);  //after choosen all the object
                    if (wrappedNotifier.isTestFailed()) {
                        wrappedNotifier.getFailure().getException().printStackTrace();
                        Scheduler.failureDetected(wrappedNotifier.getFailure().getMessage());
                        wrappedNotifier.testExplorationStarted();
                        if (!stopOnFirstError) {
                            break;
                        }else{
                            //stopOnFirstError = true;
                        }
                    }
                    // If expected deadlock but it isn't deadlocking, fail the test
                    if (isDeadlockExpected) {
                        Scheduler.failureDetected(EXPECT_DEADLOCK_MSG);
                        Scheduler.completedScheduleExecution();
                        wrappedNotifier.fireTestFailure(new Failure(describeChild(method), new RuntimeException(EXPECT_DEADLOCK_MSG)));
                        if (stopOnFirstError) {

                            break;
                        }else {
                            //stopOnFirstError = true;
                        }
                    }
                    Scheduler.completedScheduleExecution();    //one schedule completed
                }
                //all schedules have been finished
                // notify runner that exploration has completed
                Scheduler.getTerminationNotifer().release();
            }
        };
    }
}
