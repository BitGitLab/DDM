package pattern;

import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.ReadNode;
import engine.trace.WriteNode;

import javax.swing.*;
import java.util.*;

public class Pattern {
    private List<IMemNode> nodes;
    private PatternType patternType;


    public Pattern(List<IMemNode> nodes) {
        this.nodes = nodes;
        this.patternType = parsePatternType(nodes);
    }

    public PatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(PatternType patternType) {
        this.patternType = patternType;
    }
    //两个pattern类型相同
    //各个node除了tid和gid不同剩下都相同

    /**
     * 严格的判断是否两个pattern相同
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static boolean isTheSamePatternStrict(Pattern pattern1, Pattern pattern2){

        if (pattern1.getNodes().size() != pattern2.getNodes().size()) {
            return false;
        }

        if(pattern1.getPatternType() != pattern2.getPatternType()) {
            return false;
        }

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();

        for(int i = 0; i < nodes1.size(); i++) {
            if(nodes1.get(i).getType() != nodes2.get(i).getType()) {
                return  false;
            } else {
                if(!((AbstractNode)nodes1.get(i)).getLabel().equals(((AbstractNode)nodes2.get(i)).getLabel())) {
                    return false;
                }
            }

            if(!getSharedId(nodes1.get(i).getAddr()).equals(getSharedId(nodes2.get(i).getAddr()))) {
                return false;
            }
        }
        return true;
    }

    public static String getSharedId(String addr) {
        try{

            String add = addr.substring(addr.indexOf('.'));
            return add;
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println(addr);
            e.printStackTrace();
            System.exit(0);
        }

        return "";

    }

    /**
     * 宽松的判断是否是同一pattern
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static boolean isTheSamePatternLoose(Pattern pattern1, Pattern pattern2) {
        if (pattern1.getNodes().size() != pattern2.getNodes().size()) {
            return false;
        }

        if(pattern1.getPatternType() != pattern2.getPatternType()) {
            return false;
        }

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();

        if(nodes1.size() == 2) {
            return isTheSameNode(nodes1.get(0), nodes2.get(0)) && isTheSameNode(nodes1.get(1), nodes2.get(1));
        }

        // length 3 pattern must about the same variable
        if(nodes1.size() == 3) {
            return isTheSameNode(nodes1.get(0), nodes2.get(0)) && isTheSameNode(nodes1.get(2), nodes2.get(2));
        }

        if(nodes1.size() == 4) {
            return isTheSamePatternStrict(pattern1, pattern2);
        }

        return true;
    }

    private static boolean isTheSameNode(IMemNode node1, IMemNode node2) {
        if(node1.getType() != node2.getType()) {
            return  false;
        } else {
            if(!((AbstractNode)node1).getLabel().equals(((AbstractNode)node2).getLabel())) {
                return false;
            }
        }

        if(!getSharedId(node1.getAddr()).equals(getSharedId(node2.getAddr()))) {
            return false;
        }

        return true;
    }
    /**
     * factory method get all patterns appear in trace rw nodes
     * @param nodes
     * @param window
     * @return
     */
    public static List<Pattern> getPatternsFromNodes(List<IMemNode> nodes, int window ) {
        List<Pattern> patterns = new ArrayList<>();
        Map<String, List<IMemNode>> nodesByAddress = new HashMap<>();

        for(IMemNode node: nodes) {
            if(!nodesByAddress.containsKey(node.getAddr())) {
                nodesByAddress.put(node.getAddr(), new ArrayList<>());
            }

            nodesByAddress.get(node.getAddr()).add(node);
        }



        for(String key: nodesByAddress.keySet()) {
            nodes = nodesByAddress.get(key);
            for(int i = 0; i < nodes.size(); i++) {
                IMemNode node = nodes.get(i);
//                System.out.println(node);
                if (window == 0) {
                    patterns.addAll(getPatterns(nodes, node, i + 1, nodes.size()));
                } else {
                    int end = nodes.size() > i + window ? i + window : nodes.size();
                    patterns.addAll(getPatterns(nodes, node, i + 1, end));
                }
            }
        }
        return patterns;
    }

    public static List<Pattern> getPatternsFromLengthTwoPattern(List<Pattern> patterns) {
        Pattern currentPattern, nextPattern, generatedPattern;
        List<Pattern> result = new ArrayList<>();
        for(int i = 0; i < patterns.size(); i++) {
            currentPattern = patterns.get(i);
            if(currentPattern.getNodes().size() != 2) continue;
            for(int j = i + 1; j < patterns.size(); j++) {
                nextPattern = patterns.get(j);
                if(nextPattern.getNodes().size() != 2) continue;
                generatedPattern = tryConstructFalconPattern(currentPattern, nextPattern);
                if (generatedPattern != null) {
                    result.add(generatedPattern);
                }

                generatedPattern = tryConstructUnicornPattern(currentPattern, nextPattern);

                if (generatedPattern != null) {
                    result.add(generatedPattern);
                }
            }
        }

        return  result;
    }


    //TODO
    public String generateStopPattern() {
        if(this.getNodes().size() == 2) {
            return "(assert ( > x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ))\n";
        } else if(this.getNodes().size() == 3) {
            return "(assert ( or ( > x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    "( > x" + this.getNodes().get(1).getGID() + " x" +  this.getNodes().get(2).getGID() + " )))\n";
        } else if(this.getNodes().size() == 4) {
            return "(assert ( or ( > x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    " ( or  ( > x" + this.getNodes().get(1).getGID() + " x" + this.getNodes().get(2).getGID() + " ) " +
                    " ( > x" + this.getNodes().get(2).getGID() +  " x" + this.getNodes().get(3).getGID() + " ))))\n";
        }
        return "";
    }


    public String preservePattern() {
        if(this.getNodes().size() == 2) {
            return " ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) ";
        } else if(this.getNodes().size() == 3) {
            return " ( and ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    "( < x" + this.getNodes().get(1).getGID() + " x" +  this.getNodes().get(2).getGID() + " ))";
        } else if(this.getNodes().size() == 4) {
            return "( and ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    " ( and  ( < x" + this.getNodes().get(1).getGID() + " x" + this.getNodes().get(2).getGID() + " ) " +
                    " ( < x" + this.getNodes().get(2).getGID() +  " x" + this.getNodes().get(3).getGID() + " )))";
        }
        return "";
    }

    public String preservePattern(List<WriteNode> mutual_exclusive_nodes) {
        StringBuilder result = new StringBuilder();
        String base = "";
        if(this.getNodes().size() == 2) {
            base = " ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) ";
        } else if(this.getNodes().size() == 3) {
            base =  " ( and ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    "( < x" + this.getNodes().get(1).getGID() + " x" +  this.getNodes().get(2).getGID() + " ))";
        } else if(this.getNodes().size() == 4) {
            base =  "( and ( < x" + this.getNodes().get(0).getGID() + " x" + this.getNodes().get(1).getGID() + " ) " +
                    " ( and  ( < x" + this.getNodes().get(1).getGID() + " x" + this.getNodes().get(2).getGID() + " ) " +
                    " ( < x" + this.getNodes().get(2).getGID() +  " x" + this.getNodes().get(3).getGID() + " )))";
        }

        result = result.append(base);
        for(WriteNode node: mutual_exclusive_nodes) {
            long node_gid = node.getGID();
            long start = this.getNodes().get(0).getGID();
            long end = this.getNodes().get(this.getNodes().size() - 1).getGID();
            result =  new StringBuilder(" ( and  " + result + " ( or ( < x" + node_gid + " x" + start + " ) ( < x" + end + "  x" + node_gid + " ) ) )" );
        }

//        System.out.println(result.toString());

        return result.toString();
    }

    public boolean contains(Pattern others) {
        if (this.equals(others)) {
            return false;
        }


        if (this.getNodes().size() == 2) {
            return false;
        }

        if (others.getNodes().size() > 2) {
            return false;
        }

        List<IMemNode> nodes = this.getNodes();

        List<IMemNode> others_nodes = others.getNodes();

        if(nodes.size() == 3) {
            return (isTheSameNode(nodes.get(0), others_nodes.get(0)) && isTheSameNode(nodes.get(1), others_nodes.get(1)))
                    || (isTheSameNode(nodes.get(1), others_nodes.get(0)) && isTheSameNode(nodes.get(2), others_nodes.get(1)));
        }

       if(nodes.size() == 4) {
           return (isTheSameNode(nodes.get(0), others_nodes.get(0)) && isTheSameNode(nodes.get(1), others_nodes.get(1)))
                   || (isTheSameNode(nodes.get(2), others_nodes.get(0)) && isTheSameNode(nodes.get(3), others_nodes.get(1)));
       }

        return false;
    }

    /**
     * construct one length-3 pattern from two length-2 pattern
     * @param pattern1
     * @param pattern2
     * @return
     */
    public static Pattern tryConstructFalconPattern(Pattern pattern1, Pattern pattern2) {

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();
        Pattern result = null;

        if (nodes1.size() != 2 && nodes2.size() != 2) {
            return null;
        } else {
            IMemNode node1 = nodes1.get(0);
            IMemNode node2 = nodes1.get(1);
            IMemNode node3 = nodes2.get(0);
            IMemNode node4 = nodes2.get(1);

            //同一个线程的
            //rwr or www
            if(node2.getGID() == node3.getGID() && node1.getTid() == node4.getTid()) {
                result = new Pattern(Arrays.asList(node1, node2, node4));
            }
            return result;
        }

    }


    public static Pattern tryConstructUnicornPattern(Pattern pattern1, Pattern pattern2) {

        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();
        Pattern result = null;

        if (nodes1.size() != 2 || nodes2.size() != 2) {
            return null;
        } else {
            IMemNode node1 = nodes1.get(0);
            IMemNode node2 = nodes1.get(1);
            IMemNode node3 = nodes2.get(0);
            IMemNode node4 = nodes2.get(1);

            Pattern temp = null;

            //如果是针对同一变量，就返回空
            if(node1.getAddr().equals(node3.getAddr())) {
                return null;
            }

            //全部是写
            if(node1.getType() == AbstractNode.TYPE.WRITE &&
                    node2.getType() == AbstractNode.TYPE.WRITE &&
                    node3.getType() == AbstractNode.TYPE.WRITE &&
                    node4.getType() == AbstractNode.TYPE.WRITE
                    ) {

                if(node1.getTid() == node4.getTid() && node2.getTid() == node3.getTid()) {
                    if(node1.getGID() < node2.getGID() && node2.getGID() < node3.getGID() && node3.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                        temp.patternType = PatternType.W1XW2XW2YW1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node2.getGID() && node2.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node2, node4));
                        temp.patternType = PatternType.W1XW2YW2XW1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node4.getGID() && node4.getGID() < node2.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node4, node2));
                        temp.patternType = PatternType.W1XW2YW1YW2X;
                    }
                }

            }

            //rw & wr
            if(node1.getType() == AbstractNode.TYPE.WRITE &&
                    node2.getType() == AbstractNode.TYPE.READ &&
                    node3.getType() == AbstractNode.TYPE.READ &&
                    node4.getType() == AbstractNode.TYPE.WRITE
                    ) {
                if(node1.getTid() == node4.getTid() && node2.getTid() == node3.getTid()) {
                    if(node1.getGID() < node2.getGID() && node2.getGID() < node3.getGID() && node3.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                        temp.patternType = PatternType.W1XR2XR2YW1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node2.getGID() && node2.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node2, node4));
                        temp.patternType = PatternType.W1XR2YR2XW1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node4.getGID() && node4.getGID() < node2.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node4, node2));
                        temp.patternType = PatternType.W1XR2YW1YR2X;
                    }
                }
            }

            //rw & wr
            if(node1.getType() == AbstractNode.TYPE.READ &&
                    node2.getType() == AbstractNode.TYPE.WRITE &&
                    node3.getType() == AbstractNode.TYPE.WRITE &&
                    node4.getType() == AbstractNode.TYPE.READ
                    ) {

                if(node1.getTid() == node4.getTid() && node2.getTid() == node3.getTid()) {
                    if(node1.getGID() < node2.getGID() && node2.getGID() < node3.getGID() && node3.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                        temp.patternType = PatternType.R1XW2XW2YR1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node2.getGID() && node2.getGID() < node4.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node2, node4));
                        temp.patternType = PatternType.R1XW2YW2XR1Y;
                    }

                    if(node1.getGID() < node3.getGID() && node3.getGID() < node4.getGID() && node4.getGID() < node2.getGID()) {
                        temp = new Pattern(Arrays.asList(node1, node3, node4, node2));
                        temp.patternType = PatternType.W1XR2YW1YR2X;
                    }
                }

            }


            return temp;
        }
    }


    private static List<Pattern> getPatterns(List<IMemNode> nodes, IMemNode currentNode, int start, int end) {
        List<Pattern> tempPattern = new ArrayList<>();

        if (currentNode.getType() == AbstractNode.TYPE.READ) {
            for (int i = start; i < end; i++) {
                IMemNode node = nodes.get(i);

                //this line of code made our pattern generation algorithm the same as falcon implement
                if(node.getType() == AbstractNode.TYPE.READ && node.getTid() == currentNode.getTid()) {
                    break;
                }

                if (node.getType() == AbstractNode.TYPE.WRITE &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()){
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    break;
                }

                if(node.getType() == AbstractNode.TYPE.WRITE) {
                    break;
                }
            }
        } else  {
            for (int i = start; i < end; i++) {
                IMemNode node = nodes.get(i);

                // wr node
                // access the same location
                // in different thread
                if (node.getType() == AbstractNode.TYPE.READ &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()) {
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    continue;
                }

                // ww node
                if (node.getType() == AbstractNode.TYPE.WRITE &&
                        node.getAddr().equals(currentNode.getAddr()) &&
                        node.getTid() != currentNode.getTid()){
                    tempPattern.add(new Pattern(Arrays.asList(currentNode, node)));
                    break;
                }

                if(node.getType() == AbstractNode.TYPE.WRITE) {
                    break;
                }
            }
        }

        return tempPattern;
    }


    public List<IMemNode> getNodes() {
        return nodes;
    }



    private PatternType parsePatternType(List<IMemNode> nodes) {
        if (nodes.size() < 2) {
            throw new IllegalStateException("the number of nodes less than 2");
        }

        if (nodes.size() <= 3) {
            StringBuilder type = new StringBuilder();
            for (IMemNode node : nodes) {
                if (node.getType() == AbstractNode.TYPE.READ) {
                    type.append("R");
                } else if (node.getType() ==  AbstractNode.TYPE.WRITE) {
                    type.append("W");
                } else {
                    throw new IllegalArgumentException();
                }
            }
            return PatternType.valueOf(type.toString());
        }

        // length 4 pattern's type is set by others

        return null;
    }

    public static boolean isRelatedPattern(Pattern pattern1, Pattern pattern2) {
        List<IMemNode> nodes1 = pattern1.getNodes();
        List<IMemNode> nodes2 = pattern2.getNodes();

        if (nodes1.size() > nodes2.size()) {
            List<IMemNode> temp = nodes1;
            nodes1 = nodes2;
            nodes2 = temp;
        }

        if (nodes1.size() == 2 ) {
            if(nodes2.size() == 2) {

                return nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(1).getGID() == nodes2.get(1).getGID();
            }

            if(nodes2.size() == 3) {
               return  (nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                       nodes1.get(1).getGID() == nodes2.get(1).getGID()) || (
                       nodes1.get(0).getGID() == nodes2.get(1).getGID() &&
                               nodes1.get(1).getGID() == nodes2.get(2).getGID());
            }

            if(nodes2.size() == 4) {
                return  (nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(1).getGID() == nodes2.get(1).getGID()) || (
                        nodes1.get(0).getGID() == nodes2.get(2).getGID() &&
                                nodes1.get(1).getGID() == nodes2.get(3).getGID());
            }
        }

        if(nodes1.size() == 3) {
            if(nodes2.size() == 3) {
                return  ((nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(1).getGID() == nodes2.get(1).getGID()) || (
                        nodes1.get(0).getGID() == nodes2.get(1).getGID() &&
                                nodes1.get(1).getGID() == nodes2.get(2).getGID())) ||
                        ((nodes1.get(1).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(2).getGID() == nodes2.get(1).getGID()) || (
                        nodes1.get(1).getGID() == nodes2.get(1).getGID() &&
                                nodes1.get(2).getGID() == nodes2.get(2).getGID()));
            }

            if(nodes2.size() == 4) {
                return  ((nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(1).getGID() == nodes2.get(1).getGID()) || (
                        nodes1.get(0).getGID() == nodes2.get(2).getGID() &&
                                nodes1.get(1).getGID() == nodes2.get(3).getGID())) ||
                        ((nodes1.get(1).getGID() == nodes2.get(0).getGID() &&
                                nodes1.get(2).getGID() == nodes2.get(1).getGID()) || (
                                nodes1.get(1).getGID() == nodes2.get(2).getGID() &&
                                        nodes1.get(2).getGID() == nodes2.get(3).getGID()));
            }
        }

        if(nodes1.size() == 4) {
            if(nodes2.size() == 4) {
                return  ((nodes1.get(0).getGID() == nodes2.get(0).getGID() &&
                        nodes1.get(1).getGID() == nodes2.get(1).getGID()) || (
                        nodes1.get(0).getGID() == nodes2.get(2).getGID() &&
                                nodes1.get(1).getGID() == nodes2.get(3).getGID())) ||
                        ((nodes1.get(2).getGID() == nodes2.get(0).getGID() &&
                                nodes1.get(3).getGID() == nodes2.get(1).getGID()) || (
                                nodes1.get(2).getGID() == nodes2.get(2).getGID() &&
                                        nodes1.get(3).getGID() == nodes2.get(3).getGID()));
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String node_string = "";
        for(IMemNode node: nodes) {
            node_string += ("\t\t" + node + ",\n");
        }

        return "Pattern{\n" +
                "\tpatternType=" + patternType + ",\n" +
                "\tnodes={\n"+
                    node_string + "\t}\n" +
                '}';
    }
}

class IllegalPatternNodes extends Exception {
    public IllegalPatternNodes() {
    }

    public IllegalPatternNodes(String message) {
        super(message);
    }

    public IllegalPatternNodes(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPatternNodes(Throwable cause) {
        super(cause);
    }

    public IllegalPatternNodes(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

class IllegalNodeType extends Exception{

}