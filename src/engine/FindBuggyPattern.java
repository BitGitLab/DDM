package engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.exploration.JUnit4MCRRunner;
import controller.exploration.Scheduler;
import controller.scheduling.strategy.RandomStrategy;
import engine.config.Configuration;
import engine.constraints.ConstraintsBuildEngine;
import engine.trace.*;
//import org.apache.tomcat.jni.Thread;
import pattern.DDMUtil;
import pattern.LogUtils;
import pattern.Pattern;
import pattern.schedule.Schedule;

import javax.swing.*;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FindBuggyPattern {

    private static ConstraintsBuildEngine iEngine;
    private static Queue<Schedule> schedules = new ConcurrentLinkedQueue<Schedule>();

    private static Map<String, Schedule> scheduleCache = new HashMap<>();
    private static Map<String, RUNNINGRESULT> resultCache = new HashMap<>();

    private static Trace failTrace;
    private static Trace sucessTrace;

    private static boolean isDebug = true;

    private static Logger class_logger = null;

    private static ConstraintsBuildEngine getEngine(String name)
    {
        if(iEngine==null){
            Configuration config = new Configuration();
            config.tableName = name;
            config.constraint_outdir = "tmp" + System.getProperty("file.separator") + "smt";

            iEngine = new ConstraintsBuildEngine(config);//EngineSMTLIB1
        }

        return iEngine;
    }

    public static void execute(Trace successTrace, Trace failedTrace) {

        //initialize logger
        String name = JUnit4MCRRunner.method.getMethod().getDeclaringClass().getName();

        Logger logger = null;
        try{
            logger = LogUtils.getLogger(name);
            if(isDebug) {

                class_logger = logger;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //set failTrace and successTrace
        failTrace = failedTrace;
        sucessTrace = successTrace;


        long exploration_end = System.currentTimeMillis();

        //calculate pattern(f) - pattern(p)
        List<Pattern> differentPatterns = DDMUtil.getDifferentPatterns(Arrays.asList(successTrace), failedTrace);
        List<Pattern> failPatterns = DDMUtil.getAllPatterns(failTrace);
        List<Pattern> diff = differentPatterns;
        List<Pattern> allSimilar = failPatterns.stream().filter(pattern -> diff.stream()
                .filter(d -> Pattern.isTheSamePatternLoose(pattern, d)).findAny().orElse(null) != null).collect(Collectors.toList());

        StartExploring.failureRunTrace.clear();
        StartExploring.succeedTraces.clear();
        List<Pattern> result = DDPlus(differentPatterns, new ArrayList<>(), 2, failTrace, sucessTrace);

        Gson g = new Gson();
        long final_time = System.currentTimeMillis();

        if(isDebug) {
            class_logger.info("");
            class_logger.info("Summary:");
        }

        logger.info("pattern(p): " + DDMUtil.getAllPatterns(failTrace).size());
        logger.info("pattern(f): " + DDMUtil.getAllPatterns(successTrace).size());



        logger.info("Different Pattern Size: " + differentPatterns.size());
        logger.info("Real Different Pattern Size: " + allSimilar.size());

        if(differentPatterns.size() == 0) {
            System.out.println(g.toJson(failTrace.getFullTrace()));
            System.out.println(g.toJson(sucessTrace.getFullTrace()));
        }
        logger.info("----------------------------------------------------------------");
        for(Pattern p: differentPatterns) {
            replacePatternNodeAddr(p);
            logger.info(g.toJson(p));
        }

        failPatterns = DDMUtil.getAllPatterns(failTrace);
        allSimilar = failPatterns.stream().filter(pattern -> result.stream()
                .filter(d -> Pattern.isTheSamePatternLoose(pattern, d)).findAny().orElse(null) != null).collect(Collectors.toList());

        if(allSimilar.size() == 0) {
            allSimilar = result;
        }

        System.out.println("final result" + result);
        logger.info("----------------------------------------------------------------");
        logger.info("final result: " + result.size());
        logger.info("Real final result: " + allSimilar.size());
        for(Pattern p: result) {
            logger.info(g.toJson(p));
        }

        logger.info("----------------------------------------------------------------");
        logger.info("Exploration time: " + (exploration_end - Scheduler.startTime) + "ms");
        logger.info("DD          time: " + (final_time - exploration_end) + "ms");
        logger.info("Total       time: " + (final_time - Scheduler.startTime) + "ms");

//        try{
//
//            String path = LogUtils.getPath(name, "Success.json");
//            g = new GsonBuilder().create();
//            FileWriter write = new FileWriter(path);
//            g.toJson(StartExploring.succeedTraces, write);
//            write.flush();
//            write.close();
//            path = LogUtils.getPath(name, "Failed.json");
//            write = new FileWriter(path, true);
//            g.toJson(StartExploring.failureRunTrace, write);
//            write.flush();
//            write.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        synchronized (schedules) {
            schedules.notify();
        }

        RandomStrategy.DDPlusFinished = true;

    }

    /**
     * implement DD+ algorithm in paper
     * @param U
     * @param R
     * @param N
     * @param f
     * @param p
     * @return
     */
    public static List<Pattern> DDPlus(List<Pattern> U, List<Pattern> R, int N, Trace f, Trace p) {
        if(isDebug) {
            class_logger.info("###############################################");
            class_logger.info("calling DDPlus with patterns:" + U.size());
            for(Pattern pattern: U) {
                class_logger.info(pattern.toString());
            }
        }

        if (U.size() == 1) {
            return U;
        }

        List<List<Pattern>> parts = DDMUtil.patternPartition(U, N);

        List<Pattern> failedPatterns = DDMUtil.getAllPatterns(f);
        List<Pattern> passPatterns = DDMUtil.getAllPatterns(p);

        for(List<Pattern> part: parts) {
            if(isDebug) {
                class_logger.info("###############################################");
                class_logger.info("calling Test with patterns:" + part.size());
                for(Pattern pattern: part) {
                    class_logger.info(pattern.toString());
                }
            }

            List<Pattern> keep =  DDMUtil.getDifferenceOfTwoPatterns(U, part);
            RUNNINGRESULT result = test(DDMUtil.patternUnion(part, R), failedPatterns, passPatterns, f, keep);
            resultCache.put(part.toString(), result);

            if(isDebug) {
                class_logger.info("Test result: " + result);
            }

            System.out.println(result);

            if(result == RUNNINGRESULT.RESTART) {
                List<Pattern> newPatterns = DDMUtil.getDifferenceOfTwoPatterns(DDMUtil.getAllPatterns(failTrace), DDMUtil.getAllPatterns(sucessTrace));
                return DDPlus(newPatterns, new ArrayList<>(), 2, failTrace, sucessTrace);
            }

            if( result == RUNNINGRESULT.SUCCESS) {
                return DDPlus(part, R, 2, f, p);
            }
        }

//        System.exit(0);
        for(List<Pattern> part: parts) {
            RUNNINGRESULT result = resultCache.get(part.toString());

            List<Pattern> differencePatterns = DDMUtil.getDifferenceOfTwoPatterns(U, part);

            List<Pattern> union = DDMUtil.patternUnion(differencePatterns, R);
            if(isDebug) {
                class_logger.info("###############################################");
                class_logger.info("calling Test with patterns:" + union.size());
                for(Pattern pattern: union) {
                    class_logger.info(pattern.toString());
                }
            }
            List<Pattern> keep = DDMUtil.getDifferenceOfTwoPatterns(U, union);
            RUNNINGRESULT differenceResult = test(union, failedPatterns, passPatterns, f, keep);

            if(isDebug) {
                class_logger.info("Test result: " + differenceResult);
            }
            System.out.println(differenceResult);
            if(differenceResult == RUNNINGRESULT.RESTART) {
                List<Pattern> newPatterns = DDMUtil.getDifferenceOfTwoPatterns(DDMUtil.getAllPatterns(failTrace), DDMUtil.getAllPatterns(sucessTrace));
                return DDPlus(newPatterns, new ArrayList<>(), 2, failTrace, sucessTrace);
            }

            resultCache.put(differencePatterns.toString(), differenceResult);

            if(differenceResult == RUNNINGRESULT.FAIL) {

                if(result == RUNNINGRESULT.FAIL) {
                    return DDMUtil.patternUnion(DDPlus(part, DDMUtil.patternUnion(differencePatterns, R), 2, f, p),
                            DDPlus(DDMUtil.getDifferenceOfTwoPatterns(differencePatterns, R), DDMUtil.patternUnion(part, R), 2, f, p));
                }

                if(result == RUNNINGRESULT.UNKNOWN) {
                    return DDPlus(part, DDMUtil.patternUnion(differencePatterns, R), 2, f, p);
                }

            }
        }

        List<Pattern> UQuote = DDMUtil.getDifferenceOfTwoPatterns(U, R);
        List<Pattern> RQuote = R;

        for(List<Pattern> part: parts) {
            if(resultCache.get(part.toString()) == RUNNINGRESULT.SUCCESS) {
                UQuote = DDMUtil.getDifferenceOfTwoPatterns(UQuote, part);
            }

            if(resultCache.get(part.toString()) == RUNNINGRESULT.FAIL) {
                RQuote = DDMUtil.patternUnion(RQuote, part);
            }
        }

        if(N < U.size()) {
            return DDPlus(UQuote, RQuote, Math.min(UQuote.size(), N * 2), f, p);
        }

        return UQuote;
    }
    /**
     * implement test algorithm in paper
     * @param U
     * @param f
     * @param p
     * @param failedTrace
     * @return
     */
    public static RUNNINGRESULT test(List<Pattern> U, List<Pattern> f, List<Pattern> p, Trace failedTrace, List<Pattern> keepPatterns) {

//        if(resultCache.containsKey(U.toString())) {
//            return resultCache.get(U.toString());
//        }

        Vector<String> schedule = calculateSchedule(failedTrace, keepPatterns, U);


        // can not build such a schedule
        if(schedule == null) {
            if(isDebug) {
                class_logger.info("Test real result:" + RUNNINGRESULT.UNKNOWN);
            }
            return RUNNINGRESULT.UNKNOWN;
        }

        schedule = generateSchedule(schedule,failedTrace);

        Schedule newSchedule = runProgram(schedule, failedTrace, U);
        if(newSchedule.isRunSuccess()) {
            StartExploring.succeedTraces.add(newSchedule.getRealTrace());
        } else {
            StartExploring.failureRunTrace.add(newSchedule.getRealTrace());
        }
        newSchedule.getRealTrace().finishedLoading(true);
        List<Pattern> newRunPatterns = DDMUtil.getAllPatterns(newSchedule.getRealTrace());

        System.out.println("real result:" + (newSchedule.isRunSuccess() ? RUNNINGRESULT.SUCCESS : RUNNINGRESULT.FAIL));
        if(isDebug) {
            class_logger.info("Test real result:" + (newSchedule.isRunSuccess() ? RUNNINGRESULT.SUCCESS : RUNNINGRESULT.FAIL));
//            Gson g = new Gson();
//            System.out.println(g.toJson(newSchedule.getRealTrace().getFullTrace()));
        }

        scheduleCache.put(U.toString(), newSchedule);



        List<Pattern> different = DDMUtil.getDifferenceOfTwoPatterns(newRunPatterns, f);
        different = DDMUtil.getDifferenceOfTwoPatterns(different, p);

        if(isDebug) {
            class_logger.info("***********************************************");
            class_logger.info("pattern(newt) - pattern(f): " + different.size());
            for(Pattern pattern: different) {
                class_logger.info(pattern.toString());
            }
        }



        different = DDMUtil.getDifferenceOfTwoPatterns(f, newRunPatterns);
        different = DDMUtil.getDifferenceOfTwoPatterns(different, p);
        if(isDebug) {
            class_logger.info("***********************************************");
            class_logger.info("pattern(f) - pattern(newt): " + different.size());
            for(Pattern pattern: different) {
                class_logger.info(pattern.toString());
            }
        }

        Schedule temp = newSchedule;
        if(DDMUtil.isSubSet(different, U)) {
             // U == pattern(f) \ pattern(newt)
             if(DDMUtil.isSubSet(U, different)) {
                 if(!newSchedule.isRunSuccess()) {
                     different = DDMUtil.getDifferenceOfTwoPatterns(newRunPatterns, f);
                     different = DDMUtil.getDifferenceOfTwoPatterns(different, p);
                     int times = 0;
                     Trace newTrace = newSchedule.getRealTrace();
                     newTrace.finishedLoading(true);
                     while(different.size() > 0 && ++times < 3) {
                         schedule = calculateSchedule(failTrace,keepPatterns, DDMUtil.patternUnion(different, U));
                         if(schedule == null) break;
                         schedule = generateSchedule(schedule, failTrace);
                         newSchedule = runProgram(schedule, failTrace,  DDMUtil.patternUnion(different, U));
                         if(newSchedule.isRunSuccess()) {
                             StartExploring.succeedTraces.add(newSchedule.getRealTrace());
                         } else {
                             StartExploring.failureRunTrace.add(newSchedule.getRealTrace());
                         }
                         newTrace = newSchedule.getRealTrace();
                         newTrace.finishedLoading(true);
                         newRunPatterns = DDMUtil.getAllPatterns(newTrace);
                         different = DDMUtil.getDifferenceOfTwoPatterns(newRunPatterns, f);
                         different = DDMUtil.getDifferenceOfTwoPatterns(different, p);
                     }
                 }
                 if(DDMUtil.isSubSet(different, U) && DDMUtil.isSubSet(U, different)) {
                    return newSchedule.isRunSuccess() ? RUNNINGRESULT.SUCCESS : RUNNINGRESULT.FAIL;
                 } else {
                     newSchedule = temp;
                     return temp.isRunSuccess() ? RUNNINGRESULT.SUCCESS : RUNNINGRESULT.FAIL;
                 }
             } else {
                if(newSchedule.isRunSuccess()) {
                    sucessTrace = newSchedule.getRealTrace();
                    sucessTrace.finishedLoading(true);
                    return RUNNINGRESULT.RESTART;
                }
             }
        }


        different = DDMUtil.getDifferenceOfTwoPatterns(p, newRunPatterns);
        if(isDebug) {
            class_logger.info("***********************************************");
            class_logger.info("pattern(p) - pattern(newt): " + different.size());
            for(Pattern pattern: different) {
                class_logger.info(pattern.toString());
            }
        }


        different = DDMUtil.getDifferenceOfTwoPatterns(newRunPatterns, p);

        if(isDebug) {
            class_logger.info("***********************************************");
            class_logger.info("pattern(newt) - pattern(p): " + different.size());
            for(Pattern pattern: different) {
                class_logger.info(pattern.toString());
            }
        }

        if(DDMUtil.isSubSet(different, U)){
            if(!newSchedule.isRunSuccess()) {
                failTrace = newSchedule.getRealTrace();
                failTrace.finishedLoading(true);
                return RUNNINGRESULT.RESTART;
            }
        }

        return RUNNINGRESULT.UNKNOWN;

    }

    public static void store_trace(String name, Trace trace) {

    }





    public static Vector<String> generateSchedule(Vector<String> schedule, Trace trace) {

        Vector<String> result = new Vector<>();

        for (int i = 0; i < schedule.size(); i++)
        {
            String xi = schedule.get(i);
            long gid = Long.valueOf(xi.substring(1));
            long tid;
            try{

                tid = trace.getNodeGIDTIdMap().get(gid);
            } catch (NullPointerException e) {
                System.err.println(trace);
                System.err.println(trace.getNodeGIDTIdMap());
                throw e;
            }
            String name = trace.getThreadIdNameMap().get(tid);

            result.add(name);
        }

        return result;
    }

    public static void setSchedules(Queue<Schedule> queue) {

        schedules = queue;
    }

    public enum RUNNINGRESULT  {
            SUCCESS, FAIL, UNKNOWN, RESTART
    }

    private static void replacePatternNodeAddr(Pattern p) {
        HashMap<Integer, String> varIdMap = failTrace.getSharedVarIdMap();

        List<IMemNode> nodes = p.getNodes();

        for(IMemNode node: nodes) {

            String addr = null;
            try{
                addr = varIdMap.get(Integer.valueOf(Pattern.getSharedId(node.getAddr()).substring(1)));
            } catch (NumberFormatException e) {
                //do nothing
            }

            if(addr == null) continue;

            if(node.getType() == AbstractNode.TYPE.WRITE) {
                ((WriteNode)node).setAddr(addr);
            }

            if(node.getType() == AbstractNode.TYPE.READ) {
                ((ReadNode)node).setAddr(addr);
            }

        }
    }

    private static Schedule runProgram(Vector<String> schedule, Trace failedTrace, List<Pattern> U){
        Schedule newSchedule = new Schedule(failedTrace, schedule, U);
        schedules.add(newSchedule);
        try{
            synchronized (schedules) {
                schedules.notify();
            }
            synchronized (newSchedule) {

                newSchedule.wait();
            }
        } catch (InterruptedException e) {
            System.out.println("wating result failed");
            System.exit(0);
        }


        return newSchedule;
    }

    private static Vector<String> calculateSchedule(Trace failTrace, List<Pattern> keepPattern, List<Pattern> stopPattern) {
        ConstraintsBuildEngine engine = getEngine(failTrace.getApplicationName());
        engine.preprocess(failTrace);

        engine.constructSyncConstraints(failTrace, new HashSet<>(failTrace.getFullTrace()));
        engine.constructPOConstraints(failTrace, new HashSet<>(failTrace.getFullTrace()));
        Vector<String> schedule = engine.generateStopPatternSchedule(DDMUtil.getConstraint(stopPattern, keepPattern, failTrace));
        return schedule;
    }
}
