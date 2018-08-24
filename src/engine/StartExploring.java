package engine;

import java.io.FileWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.xml.internal.bind.v2.runtime.output.SAXOutput;
import controller.exploration.JUnit4MCRRunner;
import controller.exploration.Scheduler;
import controller.scheduling.strategy.MCRStrategy;
import engine.config.Configuration;
import engine.trace.*;
import pattern.DDMUtil;
import pattern.LogUtils;
import pattern.Pattern;
import pattern.schedule.Schedule;

import javax.swing.*;

public class StartExploring implements Runnable {

	private Trace traceObj;

	private Vector<String> schedule_prefix;

	private Queue<List<String>> exploreQueue;
    private static Queue<Schedule> patternSchedules;

	public static List<Trace> succeedTraces; //List save success run traces
	public static List<Trace> failureRunTrace;

	public static class BoxInt {

		volatile int  value;

		public BoxInt(int initial) {
			this.value = initial;
		}

		public synchronized int getValue() {
			return this.value;
		}

		public synchronized void increase() {
			this.value++;
		}

		public synchronized void decrease() {
			this.value--;
		}
	}

	public final static BoxInt executorsCount = new BoxInt(0);

	public StartExploring(Trace trace, Vector<String> prefix,
						  Queue<List<String>> queue,Queue<Schedule> patternSchedules,
						  List<Trace> succeedTraces,List<Trace> failureRunTrace) {

		this.traceObj = trace;
		this.schedule_prefix = prefix;
		this.exploreQueue = queue;
		this.patternSchedules = patternSchedules;
		this.succeedTraces = succeedTraces;
		this.failureRunTrace = failureRunTrace;
	}

	public Trace getTrace() {
		return this.traceObj;
	}

	public Vector<String> getCurrentSchedulePrefix() {
		return this.schedule_prefix;
	}

	public Queue<List<String>> exploreQueue() {
		return this.exploreQueue;
	}

	/**
	 * start exploring other interleavings
	 * 
	 */
	public void run() {
		try {

			/**
			 * In case succeedTraces contains failureRunTrace
			 */
			for(Trace failureTrace : failureRunTrace){
				succeedTraces.remove(failureTrace);
			}
			/**
			 * Finish loading traces
			 */
			for(Trace failureTrace : failureRunTrace){
				failureTrace.finishedLoading(true);
			}
			for (Trace succeedTrace : succeedTraces){
				succeedTrace.finishedLoading(true);
			}

			System.out.println("failureRunTrace.size:" + failureRunTrace.size());
			System.out.println("successedTraces.size:" + succeedTraces.size());

			FindBuggyPattern.setSchedules(this.patternSchedules);



			Map.Entry<Trace, Trace> testTraces = DDMUtil.getTestTraces(succeedTraces, failureRunTrace);

			FindBuggyPattern.execute(testTraces.getKey(), testTraces.getValue());


		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		finally {
			if (Configuration.DEBUG) {
				System.out.println("  Exploration Done with this trace! >>\n\n");
			}
		}
	}


}
