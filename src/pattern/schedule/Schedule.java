package pattern.schedule;

import engine.trace.Trace;
import pattern.DDMUtil;
import pattern.Pattern;

import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class Schedule{
    public Trace originalTrace;
    public Pattern stopPattern;
    public List<Pattern> stopPatterns;
    public Vector<String> schedule;
    public Trace realTrace;
    public boolean isAsSchedule;
    public boolean runSuccess;

    public Schedule(Trace originalTrace, Vector<String> schedule, Pattern stopPattern) {
        this.originalTrace = originalTrace;
        this.schedule = schedule;
        this.stopPattern = stopPattern;
        this.runSuccess = true;
    }

    public Schedule(Trace originalTrace, Vector<String> schedule, List<Pattern> stopPatterns) {
        this.originalTrace = originalTrace;
        this.schedule = schedule;
        this.stopPatterns = stopPatterns;
        this.runSuccess = true;
    }

    public Trace getOriginalTrace() {
        return originalTrace;
    }

    public Pattern getStopPattern() {
        return stopPattern;
    }

    public Vector<String> getSchedule() {
        return schedule;
    }

    public Trace getRealTrace() {
        return realTrace;
    }

    public void setRealTrace(Trace realTrace) {
        this.realTrace = realTrace;
    }

    public boolean isRunSuccess() {
        return runSuccess;
    }

    public void setRunSuccess(boolean runSuccess) {
        this.runSuccess = runSuccess;
    }

    public List<Pattern> getContainPatterns(List<Pattern> differentPattern) {
        List<Pattern> contains = DDMUtil.getAllPatterns(realTrace);

        return contains;
    }



    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
