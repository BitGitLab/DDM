package pattern;

import com.google.gson.Gson;
import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.Trace;
import engine.trace.WriteNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Map.Entry;




public class DDMUtil {
    /**
     * extract all patterns from trace
     * @param trace
     * @return
     */
    public static List<Pattern> getAllPatterns(Trace trace) {

        List<IMemNode> RWNodes = getAllRWNodes(trace);

        List<pattern.Pattern> patterns = pattern.Pattern.getPatternsFromNodes(RWNodes, 0);

        List<pattern.Pattern> falconPatterns = pattern.Pattern.getPatternsFromLengthTwoPattern(patterns);

        patterns.addAll(falconPatterns);

        return patterns;
    }


    public static List<Pattern> getAllPatterns(List<IMemNode> nodes) {
        List<pattern.Pattern> patterns = pattern.Pattern.getPatternsFromNodes(nodes, 0);

        List<pattern.Pattern> falconPatterns = pattern.Pattern.getPatternsFromLengthTwoPattern(patterns);

        patterns.addAll(falconPatterns);

        return patterns;
    }



    /**
     * get patterns appear in error patterns but not success patterns
     * @param errorPatterns
     * @param successPatterns
     * @return
     */
    public static List<pattern.Pattern> getDifferentPatterns(List<Pattern> errorPatterns, List<Pattern> successPatterns) {

        List<Pattern> finalResult  = getDifferenceOfTwoPatterns(errorPatterns, successPatterns);

        List<Pattern> toRemove = finalResult.stream().filter(pattern -> pattern.getNodes().size() == 2 &&
                finalResult.stream().filter(
                        p -> p.getNodes().size() > 2 && p.contains(pattern)
                ).findAny().orElse(null) != null).collect(Collectors.toList());

        finalResult.removeAll(toRemove);

        return finalResult;
    }


    public static List<Pattern> getDifferentPatterns(List<Trace> succeedTraces, Trace failedTrace) {

        List<Pattern> success = new ArrayList<>();
        for (Trace trace: succeedTraces) {
            success.addAll(getAllPatterns(trace));
        }
        List<Pattern> successPattern = removeDuplicatePatterns(success);

        List<Pattern> errorPattern = getAllPatterns(failedTrace);
        return getDifferentPatterns(errorPattern, successPattern);

    }

    /**
     * get all RW nodes related to shared variables
     * @param trace
     * @return
     */
    public static List<IMemNode> getAllRWNodes(Trace trace) {
        Vector<AbstractNode> nodes = trace.getFullTrace();

        HashSet<String> variables = trace.getSharedVariables();

        List<IMemNode> RWNodes = nodes.stream().filter(node -> (node.getType() == AbstractNode.TYPE.READ || node.getType() == AbstractNode.TYPE.WRITE))
                .filter(node -> !((AbstractNode)node).getLabel().equals("RVRunTime.java:621"))
                .filter(node -> variables.contains(((IMemNode)node).getAddr()))
                .map(node -> (IMemNode)node).collect(Collectors.toList());

        return RWNodes;
    }

    public static StringBuilder getStopConstraint(Pattern pattern, Trace trace) {
        List<IMemNode> nodes = getAllRWNodes(trace);

        List<Pattern> similarPatterns = Helper.getALLSimilarPatternFromNodes(pattern, nodes);


        StringBuilder stopConstraint = new StringBuilder();

        for(Pattern similar: similarPatterns) {
            stopConstraint.append(similar.generateStopPattern());
        }

        return stopConstraint;

    }

    /**
     * stop one pattern and keep some other patterns in new schedule
     * @param stopPattern
     * @param differentPatterns
     * @param trace
     * @return
     */
    public static StringBuilder getConstraint(Pattern stopPattern, List<Pattern> differentPatterns, Trace trace) {
        StringBuilder stopConstraint = getStopConstraint(stopPattern, trace);
       // return stopConstraint;

        List<Pattern> similarPatterns = Helper.getALLSimilarPatternFromNodes(stopPattern, getAllRWNodes(trace));

        StringBuilder preserveConstraint = new StringBuilder();
        List<IMemNode> rwnodes = getAllRWNodes(trace);

        for (Pattern p: differentPatterns) {
            if(!Pattern.isTheSamePatternStrict(p, stopPattern) && !isRelatedPattern(similarPatterns, p)) {
                List<Pattern> temp = Helper.getALLSimilarPatternFromNodesLoose(p, rwnodes);
                if(temp.size() > 0) {
                    preserveConstraint.append(" ( assert " + getPreservePattern(temp, rwnodes) +  " )\n");
                }
            }
        }


        return preserveConstraint.append(stopConstraint);
    }


    /**
     * get constraint that stop many patterns and keep some other patterns
     * @param stopPatterns
     * @param keepPatterns
     * @param trace
     * @return
     */
    public static StringBuilder getConstraint(List<Pattern> stopPatterns, List<Pattern> keepPatterns, Trace trace) {

        List<IMemNode> rwnodes = getAllRWNodes(trace);

        List<Pattern> allStops = new ArrayList<>();

        StringBuilder stopConstraints = new StringBuilder();
        for(Pattern pattern: stopPatterns){
            stopConstraints.append(getStopConstraint(pattern, trace));
            allStops.addAll(Helper.getALLSimilarPatternFromNodes(pattern, rwnodes));
        }


        StringBuilder preserveConstraint = new StringBuilder();
        List<IMemNode> nodes = getAllRWNodes(trace);

        for (Pattern p: keepPatterns) {
            List<Pattern> temp = Helper.getALLSimilarPatternFromNodesLoose(p, rwnodes);
            temp = temp.stream().filter(pattern -> allStops.stream().filter(stop -> Pattern.isRelatedPattern(stop, pattern)).findAny().orElse(null) == null).collect(Collectors.toList());

            if(temp.size() > 0) {
                preserveConstraint.append(" ( assert " + getPreservePattern(temp, nodes) +  " )\n");
            }
        }
        return preserveConstraint.append(stopConstraints);
    }

    /**
     * check if there is a pattern in @param{patterns} is same to p
      * @param patterns
     * @param p
     * @return
     */
    private static boolean existSamePattern(List<Pattern> patterns, Pattern p) {
        for(Pattern pattern: patterns) {
            if(Pattern.isTheSamePatternLoose(p, pattern)) {
                return true;
            }
        }

        return false;
    }


    /**
     * check if @param{pattern} has dependence relationship with pattern in @param{patterns}
     * @param patterns
     * @param pattern
     * @return
     *
     */
    public static boolean isRelatedPattern(List<Pattern> patterns, Pattern pattern) {
        for(Pattern p: patterns) {
            if(Pattern.isRelatedPattern(p, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * get the different of tow patterns that is the result of patterns1 - patterns2
     * @param patterns1
     * @param patterns2
     * @return
     */
    public static List<Pattern> getDifferenceOfTwoPatterns(List<Pattern> patterns1, List<Pattern> patterns2) {
        List<Pattern> result = patterns1.stream()
                .filter(p1 -> patterns2.stream()
                        .filter(p2 -> Pattern.isTheSamePatternLoose(p1, p2))
                        .findAny()
                        .orElse(null) == null).collect(Collectors.toList());

        List<Pattern> toRemove = result.stream().filter(pattern -> pattern.getNodes().size() == 2 &&
                result.stream().filter(
                        p -> p.getNodes().size() > 2 && p.contains(pattern)
                ).findAny().orElse(null) != null).collect(Collectors.toList());

        result.removeAll(toRemove);
        List<Pattern> finalResult = removeDuplicatePatterns(result);

        return finalResult;
    }

    public static StringBuilder getPreservePattern(List<Pattern> patterns, List<IMemNode> nodes) {
//        System.out.println(patterns.size() + "");
        StringBuilder result = new StringBuilder();
        if(patterns.size() == 0) {
            return result;
        }
        if(patterns.size() == 1) {
            return new StringBuilder(patterns.get(0).preservePattern(Helper.getRelatedWriteNode(nodes, patterns.get(0))));
        } else if (patterns.size() == 2) {
            return new StringBuilder().append(" ( or  " + patterns.get(0).preservePattern(Helper.getRelatedWriteNode(nodes, patterns.get(0))) + " " +
                    patterns.get(1).preservePattern(Helper.getRelatedWriteNode(nodes, patterns.get(0))) + "  ) ");
        } else {
            int medium = patterns.size() / 2;

            try{

                result.append(" ( or  " + getPreservePattern(patterns.subList(0, medium), nodes) + " " +
                        getPreservePattern(patterns.subList(medium, patterns.size()), nodes) + "  ) ");
            } catch (StackOverflowError error) {
                error.printStackTrace();

                System.out.println("error_pattern" + patterns);
                System.exit(0);
            }


            return result;
        }
    }

    /**
     * check if patterns1 is a subset of patterns2
     * @param patterns1
     * @param patterns2
     * @return
     */
    public static boolean isSubSet(List<Pattern> patterns1, List<Pattern> patterns2) {
        return patterns1.stream()
                .filter(pattern1 -> patterns2.stream()
                        .filter(pattern2 -> Pattern.isTheSamePatternLoose(pattern1, pattern2))
                        .findAny()
                        .orElse(null)  == null)
                .findAny()
                .orElse(null) == null;
    }


    /**
     * equally split a list into N parts
     * @param U
     * @param N
     * @return
     */
    public static List<List<Pattern>> patternPartition(List<Pattern> U, int N) {
        List<List<Pattern>> result = new ArrayList<>();
        if(N <= 1) {
            result.add(U.subList(0, U.size()));
        }

        if(U.size() <= N) {
            for(int i = 0; i < U.size(); i++) {
                result.add(U.subList(i, i + 1));
            }
        } else {
            int amount = U.size() / N;

            for(int i = 0; i < N - 1; i++) {
                result.add(U.subList(i * amount, (i + 1) * amount));
            }

            result.add(U.subList((N - 1) * amount, U.size()));
        }

        return result;
    }

    /**
     * list union function
     * @param patterns1
     * @param patterns2
     * @return
     */
    public static List<Pattern> patternUnion(List<Pattern> patterns1, List<Pattern> patterns2) {
        List<Pattern> result = new ArrayList<>();

        for(Pattern p: patterns1) {
            result.add(p);
        }

        for(Pattern p: patterns2) {
            result.add(p);
        }

        result = removeDuplicatePatterns(result);

        return result;
    }


    /**
     * remove duplicate patterns from pattern list
     * return a pattern set which each pattern is unique in the list
     * @param patterns
     * @return
     */
    public static List<Pattern> removeDuplicatePatterns(List<Pattern> patterns) {
        List<Pattern> result = new ArrayList<>();

        boolean hasSame = false;
        for(Pattern p: patterns) {
            for(Pattern r: result) {
                if(Pattern.isTheSamePatternLoose(p, r)) {
                    hasSame = true;
                    break;
                }
            }
            if(hasSame) {
                hasSame = false;
            } else {
                result.add(p);
            }
        }
        return result;
    }

    public static List<Pattern> removeDuplicatePatternsStrict(List<Pattern> patterns) {
        List<Pattern> result = new ArrayList<>();

        boolean hasSame = false;
        for(Pattern p: patterns) {
            for(Pattern r: result) {
                if(Pattern.isTheSamePatternStrict(p, r)) {
                    hasSame = true;
                    break;
                }
            }
            if(hasSame) {
                hasSame = false;
            } else {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * select two traces as DDPlus method input
     * in this version, this method select two traces that are furthest away
     * @param successTraces
     * @param errorTraces
     * @return
     */
    public static Entry<Trace, Trace> getTestTraces(List<Trace> successTraces, List<Trace> errorTraces) {
        int differentSize = Integer.MIN_VALUE;
        Entry<Trace, Trace> result = null;
        for(Trace s: successTraces) {
            for(Trace e: errorTraces) {
                int temp = getDifferentPatterns(Arrays.asList(s), e).size();
//                System.out.println(temp);
                if(temp > differentSize && temp > 0) {
                    differentSize = temp;
                    result = new AbstractMap.SimpleEntry(s, e);
                }
            }
        }
        //this means all different pattern size is zero
        if(result == null) {
            result = new AbstractMap.SimpleEntry<>(successTraces.get(0), errorTraces.get(0));
        }

        return result;
    }

    /**
     * return two patterns' intersection
     * @param patterns1
     * @param patterns2
     * @return
     */
    public static List<Pattern> getIntersectionOfTwoPatterns(List<Pattern> patterns1, List<Pattern> patterns2) {
        List<Pattern> result = patterns1.stream().filter(
                p1-> patterns2.stream().filter(
                        p2-> Pattern.isTheSamePatternLoose(p1, p2)
                ).findAny().orElse(null) != null
        ).collect(Collectors.toList());


        return result;
    }


}
