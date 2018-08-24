package controller.scheduling.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.Instrumentor.RVGlobalStateForInstrumentation;
import controller.Instrumentor.RVRunTime;
import controller.MCRProperties;
import controller.exploration.JUnit4MCRRunner;
import controller.exploration.Scheduler;
import controller.scheduling.ChoiceType;
import controller.scheduling.ThreadInfo;
import controller.scheduling.events.EventType;
import engine.StartExploring;
import engine.config.Configuration;
import engine.trace.Trace;
import engine.trace.TraceInfo;
import pattern.LogUtils;
import pattern.Pattern;
import pattern.schedule.Schedule;

import java.io.FileWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RandomStrategy extends SchedulingStrategy {

	protected Queue<List<String>> toExplore;

	public static List<Integer> choicesMade;

	public static List<String> schedulePrefix = new ArrayList<String>();

    public static Trace currentTrace;

	private boolean notYetExecutedFirstSchedule;

	private final static int NUM_THREADS = 10;

	public volatile static ExecutorService executor;

	public final static boolean fullTrace;

    protected ThreadInfo previousThreadInfo;

    private static Queue<Schedule> toSchedules = new ConcurrentLinkedQueue<Schedule>(); //Schedules obtained from pattern

	private static List<String> prefix;

    private static Schedule currentSchedule;

    private static List<Trace> succeedTraces; //List save success run traces

    private static List<Trace> failureRunTrace;

	private static List<Schedule> alreadyExploredSchedules = new ArrayList<>();

	private static Random rand = new Random();


	public static  List<Pattern> differentPatterns;

	public static boolean DDPlusFinished = false;

	static {
		fullTrace = Boolean.parseBoolean(MCRProperties.getInstance()
				.getProperty(MCRProperties.RV_CAUSAL_FULL_TRACE, "false"));
	}

	@Override
	/**
	 * Called before a new exploration starts
	 *  do some initial work for exploring
	 */
	public void startingExploration() {

		this.toExplore = new ConcurrentLinkedQueue<List<String>>();
		this.toSchedules = new ConcurrentLinkedQueue<Schedule>();
		this.alreadyExploredSchedules = new ArrayList<>();

		RandomStrategy.choicesMade = new ArrayList<Integer>();
		RandomStrategy.schedulePrefix = new ArrayList<String>();
		this.notYetExecutedFirstSchedule = true;
		RVRunTime.currentIndex = 0;
		executor = Executors.newFixedThreadPool(NUM_THREADS);
		succeedTraces = new ArrayList<Trace>();
		failureRunTrace = new ArrayList<Trace>();
	}

	/**
	 * called before a new schedule starts
	 */
	@Override
	public void startingScheduleExecution() {

        Scheduler.logFile = "";

	    if(!this.toSchedules.isEmpty()){

	        currentSchedule = this.toSchedules.poll();
	        prefix = currentSchedule.getSchedule();
        }else{
            prefix = new ArrayList<>();
        }

        Scheduler.logFile = Scheduler.logFile + "current schedule:" + prefix + "\n";

		if (!RandomStrategy.choicesMade.isEmpty()) {   // when not empty
			RandomStrategy.choicesMade.clear();
			RandomStrategy.schedulePrefix = new ArrayList<String>();
			for (String choice : prefix) {
				RandomStrategy.schedulePrefix.add(choice);
			}
		}
		
		RVRunTime.currentIndex = 0;
		RVRunTime.failure_trace.clear();
		initTrace();
		
        previousThreadInfo = null;
	}
	
    public static Trace getTrace() {
        return currentTrace;
    }
    
    //problem here
    //in the first execution, the initialized trace will be used by the aser-engine project
    //however, in the first initialization, the trace hasn't been complete yet.
	private void initTrace() {
	    
       RVRunTime.init();
       TraceInfo traceInfo = new TraceInfo(
                RVGlobalStateForInstrumentation.variableIdSigMap,
                new HashMap<Integer, String>(), 
                RVGlobalStateForInstrumentation.stmtIdSigMap,
                RVRunTime.threadTidNameMap);
       traceInfo.setVolatileAddresses(RVGlobalStateForInstrumentation.instance.volatilevariables);
       currentTrace = new Trace(traceInfo);
	}

	int i  = 1 ;
	@Override
	public void completedScheduleExecution() {

//		this.notYetExecutedFirstSchedule = false;

		Vector<String> prefix = new Vector<String>();
		for (String choice : RandomStrategy.schedulePrefix) {
			prefix.add(choice);
		}

        Scheduler.logFile = Scheduler.logFile + "<< Exploring trace executed along causal schedule  " + i + ": " + choicesMade + "\n";
        System.err.println(Scheduler.logFile);
        i++;

		if(!Scheduler.isFailureDetected()){
			succeedTraces.add(currentTrace);
		}
		//executeMultiThread(trace, prefix);
		/*
		 * after executing the program along the given prefix
		 * then the model checker will analyze the trace generated 
		 * to computer more possible interleavings
		 */
		if(currentSchedule != null){

			currentTrace.getTraceInfo().updateIdSigMap( RVGlobalStateForInstrumentation.stmtIdSigMap );
			currentTrace.finishedLoading(true);
			currentSchedule.setRealTrace(currentTrace);
			alreadyExploredSchedules.add(currentSchedule);
			synchronized (currentSchedule) {
                currentSchedule.notify();
            }
		}else{

			if(!succeedTraces.isEmpty() && !failureRunTrace.isEmpty() && Scheduler.hasReachTimeLimit() && this.notYetExecutedFirstSchedule){

			    System.err.println("execute single be called");
				executeSingleThread(prefix);
				this.notYetExecutedFirstSchedule = false;
			}
		}
	}

	@Override
	public void completedExploration(){

		for(int i = 0;i < alreadyExploredSchedules.size(); i ++){

			if(alreadyExploredSchedules.get(i).isRunSuccess()){
				System.out.println("Schedule: " + (i + 1) + "  run success" );
			}else{
				System.out.println("Schedule: " + (i + 1) + "  run failed" );
			}
		}

		String name = JUnit4MCRRunner.method.getMethod().getDeclaringClass().getName();
		Gson g = new Gson();
		try{

			String path = LogUtils.getPath(name, "Success.json");
			g = new GsonBuilder().create();
			FileWriter write = new FileWriter(path);
			g.toJson(StartExploring.succeedTraces, write);
			write.flush();
			write.close();
			path = LogUtils.getPath(name, "Failed.json");
			write = new FileWriter(path, true);
			g.toJson(StartExploring.failureRunTrace, write);
			write.flush();
			write.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void failureDetected(){

	    if(currentSchedule!=null && currentSchedule.getSchedule().equals(prefix)){

	    	currentSchedule.setRunSuccess(false);
		}
		failureRunTrace.add(currentTrace);
    }
	
	/**
	 * here creates a runnable object and it can then run the method 
	 * @param prefix
	 */
	//已经获得一条trace ， 开始新的探索
	private void executeSingleThread(Vector<String> prefix) {
	    
	    currentTrace.getTraceInfo().updateIdSigMap( RVGlobalStateForInstrumentation.stmtIdSigMap );   //solving the first trace initialization problem

		StartExploring causalTrace = new StartExploring(currentTrace, prefix, this.toExplore,this.toSchedules,succeedTraces,failureRunTrace);
		Thread causalTraceThread = new Thread(causalTrace);
		causalTraceThread.start();
		try {
		//	causalTraceThread.join();
            synchronized (this.toSchedules) {
                this.toSchedules.wait();
            }
//			if(succeedTraces.size() > 0){
//				this.notYetExecutedFirstSchedule = false;
//			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean canExecuteMoreSchedules() {

		boolean result = failureRunTrace.isEmpty() || succeedTraces.isEmpty() || !Scheduler.hasReachTimeLimit()
				|| this.notYetExecutedFirstSchedule || (!this.toSchedules.isEmpty()) || !DDPlusFinished;
		if (result) {
			return true;
		}

		while (StartExploring.executorsCount.getValue() > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		result = (!this.toExplore.isEmpty()) || this.notYetExecutedFirstSchedule;
		return result;
	}

	@Override
	/**
	 * choose the next statement to execute
	 */
	public Object choose(SortedSet<? extends Object> objectChoices,ChoiceType choiceType) {
		/*
		 * Initialize choice
		 */
		int chosenIndex = 0;
		Object chosenObject = null;

		if (RandomStrategy.schedulePrefix.size() > RVRunTime.currentIndex) {
			/*
			 * Make the choice to be made according to schedule prefix
			 */
			chosenIndex = getChosenThread(objectChoices, RVRunTime.currentIndex);
			chosenObject = getChosenObject(chosenIndex, objectChoices);
			
			if (Configuration.DEBUG) {
			    if (chosenObject != null) 
                System.out.println(RVRunTime.currentIndex + ":" + chosenObject.toString());
            }
			
			if (chosenObject == null) {
			    
			    //one case that can cause this is due to the wait event
			    //wait has no corresponding schedule index, it has to be announced 
			    //chose the wait to execute, the wait is trying to acquire the semaphore
			    for (Iterator<? extends Object> iterator = objectChoices.iterator(); iterator.hasNext();) {
                     ThreadInfo threadInfo = (ThreadInfo) iterator.next();
                    if(threadInfo.getEventDesc().getEventType() == EventType.WAIT){
                        return threadInfo;
                    }
                }
			    
			    //what if the chosenObject is still null??
			    //it might not correct
			    if (chosenObject == null) {
		            chosenIndex = 0;
		            while (true) {
		                chosenObject = getChosenObject(chosenIndex, objectChoices);
		                  
		                if(choiceType.equals(ChoiceType.THREAD_TO_FAIR) && chosenObject.equals(previousThreadInfo)) {
		                    //change to a different thread
		                }
		                else {
                            break;
                        }
		                chosenIndex++;
		                
		            }
		            
		        }
		        RandomStrategy.choicesMade.add(chosenIndex);
		                
		        this.previousThreadInfo = (ThreadInfo) chosenObject;
                return chosenObject;
            }
			
		}
		
		//it might be that the wanted thread is blocked, waiting to be added to the paused threads
		if (chosenObject == null) {

		    boolean firstChoose = true;
		    chosenIndex = rand.nextInt(objectChoices.size());
//		    System.out.println(objectChoices.size() + " " + chosenIndex);
			//chosenIndex = 0;
			while (true) {

			    chosenObject = getChosenObject(chosenIndex, objectChoices);
                if(choiceType.equals(ChoiceType.THREAD_TO_FAIR) && chosenObject.equals(previousThreadInfo)) {
                    //change to a different thread
                    //System.out.println("change to a different thread");
                    if(firstChoose){
                        chosenIndex = -1;
                        firstChoose = false;
                    }
                } else {
                    break;
                }
                chosenIndex++;
            }
		}
		
		RandomStrategy.choicesMade.add(chosenIndex);
        		
		this.previousThreadInfo = (ThreadInfo) chosenObject;
		
		return chosenObject;
	}

	@Override
	public List<Integer> getChoicesMadeDuringThisSchedule() {
		return RandomStrategy.choicesMade;
	}
	
	
	/**
	 * chose a thread object based on the index
	 * return -1 if not found
	 * @param objectChoices
	 * @param index
	 * @return
	 */
	private int getChosenThread(SortedSet<? extends Object> objectChoices,int index) {

		String name = RandomStrategy.schedulePrefix.get(index).split("_")[0];
		long tid = -1;
		for (Entry<Long, String> entry : RVRunTime.threadTidNameMap.entrySet()) {

			if (name.equals(entry.getValue())) {
				tid = entry.getKey();
				break;
			}
		}

		Iterator<? extends Object> iter = objectChoices.iterator();
		int currentIndex = -1;
		while (iter.hasNext()) {
			++currentIndex;
			ThreadInfo ti = (ThreadInfo) iter.next();
			if (ti.getThread().getId() == tid) {
				return currentIndex;
			}
		}
		return -1;
	}
}
