package pattern;

import com.alibaba.fastjson.JSONReader;
import com.google.gson.*;
import engine.trace.*;
import test.utils.resultStatistics;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class UnicornUtil {

    public static int failedtime = 0;
    public static Map<String, String> variableMap = null;
    public static int successTraceSize = 0;
    public static int failTraceSize = 0;
    private static final int SUCCESS_TRACE_SIZE_LIMIT = 9999;
    private static String systemType = System.getProperty("os.name");

    public static void main(String[] args) {
        String path = "";
        String root = "";
        String resultPath = "";

        if (systemType.contains("Mac")) {
            root = "./result";
            resultPath = "./codes/result";
        }else if (systemType.contains("Windows")) {
            root = "D:\\\\codes\\\\java\\\\FMCR\\\\result";
            resultPath = "D:\\\\codes/result";
        }else {
            throw new RuntimeException("unknown system type");
        }

        List<String> projects = resultStatistics.getAllProjects(root);
        for(String project: projects) {
            System.out.println(project);
            path = root + "/" + project;


            try{
                FileWriter writer = new FileWriter(resultPath + "/" + project + ".log");
                FileWriter writer1 = new FileWriter(resultPath + "/" + project + "_Unicron.log");
                failTraceSize = 0;
                successTraceSize = 0;
                variableMap = null;
                failedtime = 0;
                List<Map.Entry<Pattern, Double>> unicornResult = unicron(path);
                List<Pattern> ddPlusResult = getDDPlusReslt(path);

                writer.write("Fail Traces size: " + failTraceSize + "\n");
                writer.write("Success Traces size: " + successTraceSize + "\n");
                writer1.write("Fail Traces size: " + failTraceSize + "\n");
                writer1.write("Success Traces size: " + successTraceSize + "\n");
                writer.write("Unicorn patterns size: " + unicornResult.size() + "\n");
                writer1.write("Unicorn patterns size: " + unicornResult.size() + "\n");
                writer.write("DDPlus patterns size: " + ddPlusResult.size() + "\n");

                writer.write("Index for each pattern in DDPlus:" + "\n");
                for(Pattern p: ddPlusResult) {
                    writer.write(p.toString() + "\n");
                    writer.write("index: " + getIndexOfPatternInUnicorn(unicornResult, p) + "\n");
                }
                for(Map.Entry<Pattern, Double> mapping: unicornResult){
                    writer1.write(mapping.getKey()+"\n"+ mapping.getValue() + "\n");
                }

                writer.flush();
                writer.close();
                writer1.flush();
                writer1.close();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    public static int getIndexOfPatternInUnicorn(List<Map.Entry<Pattern, Double>> unicornResult, Pattern p) {
        int index = 0;
        for(Map.Entry<Pattern, Double> mapping: unicornResult){
            index++;
            if(Pattern.isTheSamePatternStrict(mapping.getKey(), p)) {
                return index;
            }
        }

        return Integer.MAX_VALUE;

    }

    public static Pattern getPatternFromCounter(Map<Pattern, Integer> counter, Pattern pattern) {
        for(Pattern p: counter.keySet()) {
            if(Pattern.isTheSamePatternStrict(p, pattern)) {
                return p;
            }
        }

        return null;
    }

    public static List<Map.Entry<Pattern, Double>> unicron(String path) {
        List<List<Pattern>> patterns = readAllPatterns(path);
        List<Pattern> allPatterns = new ArrayList<>();
        List<Pattern> successPatterns = patterns.get(0);
        List<Pattern> failPatterns = patterns.get(1);
        allPatterns.addAll(patterns.get(0));
        allPatterns.addAll(patterns.get(1));

        Map<Pattern, Integer> failCounter = new HashMap<>();
        Map<Pattern, Integer> successCounter = new HashMap<>();

        Map<Pattern, Double> finalResult = new HashMap<>();

        for(Pattern p: successPatterns) {
            Pattern temp = getPatternFromCounter(successCounter, p);
            if(temp != null) {
                successCounter.put(temp, successCounter.get(temp) + 1);
            } else {
                successCounter.put(p, 1);
            }
        }

        for(Pattern p: failPatterns) {
            Pattern temp = getPatternFromCounter(failCounter, p);
            if(temp != null) {
                failCounter.put(temp, failCounter.get(temp) + 1);
            } else {
                failCounter.put(p, 1);
            }
        }

        allPatterns = DDMUtil.removeDuplicatePatternsStrict(allPatterns);


        for(Pattern p: allPatterns) {
            Pattern temp1 = getPatternFromCounter(successCounter, p);
            Pattern temp2 = getPatternFromCounter(failCounter, p);
            int pass = 0;
            int fail = 0;

            if(temp1 != null) {
                pass = successCounter.get(temp1);
            }

            if(temp2 != null) {
                fail = failCounter.get(temp2);
            }

            try {
                double suspiciousness =  fail * 1.0 / (pass + failedtime);
                finalResult.put(p, suspiciousness);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(p);
            }
        }
        List<Map.Entry<Pattern, Double>> infoIds = new ArrayList<Map.Entry<Pattern, Double>>(finalResult.entrySet());
        Collections.sort(infoIds, new Comparator<Map.Entry<Pattern, Double>>() {
            public int compare(Map.Entry<Pattern, Double> o1,
                               Map.Entry<Pattern, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

//        for(Map.Entry<Pattern, Double> mapping:infoIds){
//            System.out.println(mapping.getKey()+":"+mapping.getValue());
//        }
        return infoIds;

    }

    public static List<Pattern> getDDPlusReslt(String path) {
        List<Pattern> patterns = new ArrayList<>();
        File file = new File(path);
        if(file.exists()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    String ddPath = f.getAbsolutePath().toString();
                    patterns.addAll(getPatternsFromDDPlusResult(ddPath + "/result.log"));
                }
            }
        }

        patterns = DDMUtil.removeDuplicatePatternsStrict(patterns);
        return patterns;
    }


    public static List<Pattern> getPatternsFromDDPlusResult(String path) {
        List<Pattern> result = new ArrayList<>();
        try {
            System.out.println(path);
            String content = readFile(path, Charset.forName("utf8"));
            content = content.substring(content.indexOf("Real final result:"));
            String lines[] =  content.split("\n");
            for(String line: lines) {
                if(line.startsWith("{\"nodes")) {
                    JsonObject object = new JsonParser().parse(line).getAsJsonObject();
                    JsonArray array = object.get("nodes").getAsJsonArray();
                    List<IMemNode> nodes = new ArrayList<>();
                    for(JsonElement e: array){
                        IMemNode node = (IMemNode)Reader.getNodeFromJsonObject(e.getAsJsonObject());
                        if(variableMap.get(node.getAddr()) != null) {
                            if (node.getType() == AbstractNode.TYPE.READ) {
                                ((ReadNode) node).setAddr("." + variableMap.get(node.getAddr()));
                            } else {
                                ((WriteNode) node).setAddr("." + variableMap.get(node.getAddr()));
                            }
                        } else {
                            System.out.println(variableMap);
                        }
                        nodes.add(node);
                    }

                    PatternType type = PatternType.getType(object.get("patternType").getAsString());

                    Pattern temp = new Pattern(nodes);
                    temp.setPatternType(type);
                    result.add(temp);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static List<Pattern> readFailed(String path){

        List<Pattern> patterns = new ArrayList<>();
        System.out.println(path);
        String content = Reader.readFromFile(path);

        JsonArray array = new JsonParser().parse(content).getAsJsonArray();
        for(JsonElement element : array) {
            failTraceSize++;
            JsonObject object = element.getAsJsonObject();

            if(variableMap == null) {
                variableMap = Reader.getSharedVariableMap(object);
                variableMap = variableMap.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            }

            List<AbstractNode> nodes = Reader.getNodesFromObject(object);
            JsonArray variables = object.get("sharedAddresses").getAsJsonArray();
            List<String> var = new ArrayList<>();

            for(JsonElement e: variables) {
                var.add(e.getAsString());
            }

            List<IMemNode> RWNodes = getALLRWNodes(nodes, var);

            List<Pattern> failedPattern = DDMUtil.getAllPatterns(RWNodes);
            patterns.addAll(failedPattern);

        }
        return patterns;
    }

    public static List<List<Pattern>> readAllPatterns(String path) {

        List <Pattern> result = new ArrayList<>();
        List <Pattern> failPatterns = new ArrayList<>();
        File file = new File(path);
        if(file.exists()) {
            File[] files = file.listFiles();
            for(File f: files) {
                if(f.isDirectory()) {
                    String successPath = f.getAbsolutePath().toString();

                    result.addAll(readSuccess(successPath + "/resultSuccess.json"));

                    failPatterns.addAll(readFailed(successPath + "/resultFailed.json"));
                    failedtime++;

                }
            }

        }
        return Arrays.asList(result, failPatterns);

    }

    public static List<Pattern> readSuccess(String path) {
        System.out.println(path);
        List<Pattern> patterns = new ArrayList<>();
        JSONReader reader = null;
        try {
            reader = new JSONReader(new FileReader(path));
            reader.startArray();
            while (reader.hasNext()) {
                if(successTraceSize > SUCCESS_TRACE_SIZE_LIMIT) {
                    break;
                }
                successTraceSize++;

                String str = reader.readString();
                JsonObject object =  new JsonParser().parse(str).getAsJsonObject();
                List<AbstractNode> nodes = Reader.getNodesFromObject(object);
                JsonArray variables = object.get("sharedAddresses").getAsJsonArray();
                List<String> var = new ArrayList<>();

                for(JsonElement e: variables) {
                    var.add(e.getAsString());
                }

                List<IMemNode> RWNodes = getALLRWNodes(nodes, var);

                List<Pattern> successPattern = DDMUtil.getAllPatterns(RWNodes);
                patterns.addAll(successPattern);
            }

            reader.endArray();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("read Success end");

        return patterns;

    }


    private static List<IMemNode> getALLRWNodes(List<AbstractNode> nodes, List<String> variables) {

        List<IMemNode> RWNodes = nodes.stream().filter(node -> (node.getType() == AbstractNode.TYPE.READ || node.getType() == AbstractNode.TYPE.WRITE))
                .filter(node -> !((AbstractNode)node).getLabel().equals("RVRunTime.java:621"))
                .filter(node -> variables.contains(((IMemNode)node).getAddr()))
                .map(node -> (IMemNode)node).collect(Collectors.toList());

        return RWNodes;
    }
}
