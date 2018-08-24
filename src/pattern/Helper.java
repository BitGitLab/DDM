package pattern;

import com.google.gson.Gson;
import engine.trace.AbstractNode;
import engine.trace.IMemNode;
import engine.trace.ReadNode;
import engine.trace.WriteNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Helper {

    /**
     * get all similar nodes
     * @param node
     * @param nodes
     * @param withLabel whether cosidering line number info
     * @return
     */
    private static List<IMemNode> getAllSimilarNodes(IMemNode node, List<IMemNode> nodes, boolean withLabel) {
        List<IMemNode> result = new ArrayList<>();
        //the node itself is a similar node of itself
        result.add(node);
        if(withLabel) {
            for(IMemNode iMemNode : nodes) {
                if(isSimilarNode(iMemNode, node)) {
                    result.add(iMemNode);
                }
            }
        } else {
            for(IMemNode iMemNode : nodes) {
                if(isSimilarNodeWithoutLabel(iMemNode, node)) {
                    result.add(iMemNode);
                }
            }
        }

        List<IMemNode> finalResult = new ArrayList<>();
        for(IMemNode n: result) {
            IMemNode temp = null;
            for(IMemNode fn: finalResult) {
                temp = fn;
                if(n.getGID() == fn.getGID()) {
                    break;
                }
            }

            if(temp == null) {
                finalResult.add(n);
            }

            if(temp != null && temp.getGID() != n.getGID()) {
                finalResult.add(n);
            }
        }

        return finalResult;
    }


    private static boolean isSimilarNode(IMemNode node1, IMemNode node2) {
        if (!(node1.getType() == node2.getType() && node1.getAddr().equals(node2.getAddr()))) {
            return false;
        }

        if(node1.getType() == AbstractNode.TYPE.READ) {
            return ((ReadNode)node1).getLabel().equals(((ReadNode)node2).getLabel());
        } else {
            return ((WriteNode)node1).getLabel().equals(((WriteNode)node2).getLabel());
        }
    }

    private static  boolean isSimilarNodeWithoutLabel(IMemNode node1, IMemNode node2) {
        return node1.getType() == node2.getType() && node1.getAddr().equals(node2.getAddr());
    }

    public static List<Pattern> getALLSimilarPatternFromNodes(Pattern pattern, List<IMemNode> nodes) {

        List<IMemNode> patternNodes = pattern.getNodes();
        List<Pattern> patterns = new ArrayList<>();
        if(patternNodes.size() == 2) {

            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);


            List<IMemNode> similar1 = getAllSimilarNodes(node1, nodes, true);
            List<IMemNode> similar2 = getAllSimilarNodes(node2, nodes, true);
            for (IMemNode similarnode1 : similar1) {
                for (IMemNode similarNode2 : similar2) {
                    if(similarNode2.getTid() != similarnode1.getTid() && similarnode1.getAddr().equals(similarNode2.getAddr())) {
                        patterns.add(new Pattern(Arrays.asList(similarnode1, similarNode2)));
                    }
                }
            }

        } else if(patternNodes.size() == 3) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);
            IMemNode node3 = patternNodes.get(2);

            List<IMemNode> similar1 = getAllSimilarNodes(node1, nodes, true);
            List<IMemNode> similar3 = getAllSimilarNodes(node3, nodes, true);


            // get simialr nodes without considering label(line number info of the node)
            List<IMemNode> similar2 = getAllSimilarNodes(node2, nodes, false);

            for (IMemNode similarnode1 : similar1) {
                for (IMemNode similarNode3 : similar3) {
                    if(similarnode1.getTid() == similarNode3.getTid() && similarnode1.getGID() < similarNode3.getGID() && similarnode1.getAddr().equals(similarNode3.getAddr())) {
                        for(IMemNode similarNode2: similar2) {
                            if(similarNode2.getTid() != similarnode1.getTid() && similarNode2.getAddr().equals(similarnode1.getAddr())) {
                                patterns.add(new Pattern(Arrays.asList(similarnode1, similarNode2, similarNode3)));
                            }
                        }

                    }
                }
            }
        } else if(patternNodes.size() == 4) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);
            IMemNode node3 = patternNodes.get(2);
            IMemNode node4 = patternNodes.get(3);

            List<IMemNode> similar1 = getAllSimilarNodes(node1, nodes, true);
            List<IMemNode> similar3 = getAllSimilarNodes(node3, nodes, true);
            List<IMemNode> similar2 = getAllSimilarNodes(node2, nodes, true);
            List<IMemNode> similar4 = getAllSimilarNodes(node4, nodes, true);
            Pattern temp = null;
            //1221
            if(node2.getTid() == node3.getTid()) {
                for(IMemNode similarNode1: similar1) {
                    for(IMemNode similarNode4: similar4) {
                        if(similarNode1.getTid() == similarNode4.getTid() && similarNode1.getGID() < similarNode4.getGID()) {
                            for(IMemNode similarNode2: similar2) {
                                if(similarNode2.getTid() != similarNode1.getTid()) {
                                    for(IMemNode similarNode3: similar3) {
                                        if(similarNode2.getTid() == similarNode3.getTid() && similarNode2.getGID() < similarNode3.getGID()) {
                                            temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                                            temp.setPatternType(pattern.getPatternType());
                                            patterns.add(temp);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //1212
            if(node2.getTid() != node3.getTid()) {
                for(IMemNode similarNode1: similar1) {
                    for(IMemNode similarNode3: similar3) {
                        if(similarNode1.getTid() == similarNode3.getTid() && similarNode1.getGID() < similarNode3.getGID()) {
                            for(IMemNode similarNode2: similar2) {
                                if(similarNode2.getTid() != similarNode1.getTid()) {
                                    for(IMemNode similarNode4: similar4) {
                                        if(similarNode2.getTid() == similarNode4.getTid() && similarNode2.getGID() < similarNode4.getGID()) {
                                            temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                                            temp.setPatternType(pattern.getPatternType());
                                            patterns.add(temp);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return removeSamePattern(patterns);

    }

    public static List<Pattern> getALLSimilarPatternFromNodes(List<Pattern> patterns, List<IMemNode> nodes) {
        List<Pattern> result = new ArrayList<>();

        for(Pattern pattern: patterns) {
            result.addAll(getALLSimilarPatternFromNodes(pattern, nodes));
        }

        return result;
    }


    public static List<IMemNode> getAllSimilarNodesLoose(IMemNode node, List<IMemNode> nodes, boolean withLabel) {
        List<IMemNode> result = new ArrayList<>();

        for(IMemNode n: nodes) {
            if(isSimilarNodeLoose(n, node, withLabel)) {
                result.add(n);
            }
        }

        return result;
    }

    public static List<Pattern> removeSamePattern(List<Pattern> patterns) {
        List<Pattern> finalResult = new ArrayList<>();

        boolean hasSame = false;
        for(Pattern p: patterns){
            for (Pattern fp: finalResult) {
                if(Pattern.isTheSamePatternStrict(p, fp)) {
                   List<IMemNode> nodes1 = p.getNodes();
                   List<IMemNode> nodes2 = fp.getNodes();
                   boolean same = true;
                   for(int i = 0; i < nodes1.size(); i++) {
                       if(nodes1.get(i).getGID() != nodes2.get(i).getGID()) {
                           same = false;
                           break;
                       }
                   }

                   if(same){
                       hasSame = true;
                       break;
                   }
                }
            }

            if(hasSame) {
                hasSame = false;
            } else {
                finalResult.add(p);
            }
        }

        return  finalResult;
    }

    public static boolean isSimilarNodeLoose(IMemNode node1, IMemNode node2, boolean withLabel) {
        if(!withLabel){
            return node1.getType() == node2.getType() && Pattern.getSharedId(node1.getAddr()).equals(Pattern.getSharedId(node2.getAddr()));
        } else {
            if (!(node1.getType() == node2.getType() && Pattern.getSharedId(node1.getAddr()).equals(Pattern.getSharedId(node2.getAddr())))) {
                return false;
            }

            if(node1.getType() == AbstractNode.TYPE.READ) {
                return ((ReadNode)node1).getLabel().equals(((ReadNode)node2).getLabel());
            } else {
                return ((WriteNode)node1).getLabel().equals(((WriteNode)node2).getLabel());
            }
        }
    }

    public static List<Pattern> getALLSimilarPatternFromNodesLoose(Pattern pattern, List<IMemNode> nodes) {
        List<IMemNode> patternNodes = pattern.getNodes();
        List<Pattern> patterns = new ArrayList<>();
        if(patternNodes.size() == 2) {

            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);


            List<IMemNode> similar1 = getAllSimilarNodesLoose(node1, nodes, true);
            List<IMemNode> similar2 = getAllSimilarNodesLoose(node2, nodes, true);
            for (IMemNode similarNode1 : similar1) {
                for (IMemNode similarNode2 : similar2) {
                    if(similarNode2.getTid() != similarNode1.getTid() && similarNode1.getAddr().equals(similarNode2.getAddr())) {
                        patterns.add(new Pattern(Arrays.asList(similarNode1, similarNode2)));
                    }
                }
            }

        } else if(patternNodes.size() == 3) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);
            IMemNode node3 = patternNodes.get(2);

            List<IMemNode> similar1 = getAllSimilarNodesLoose(node1, nodes, true);
            List<IMemNode> similar3 = getAllSimilarNodesLoose(node3, nodes, true);


            // get simialr nodes without considering label(line number info of the node)
            List<IMemNode> similar2 = getAllSimilarNodesLoose(node2, nodes, false);

            for (IMemNode similarNode1 : similar1) {
                for (IMemNode similarNode3 : similar3) {
                    if(similarNode1.getTid() == similarNode3.getTid() &&  similarNode1.getAddr().equals(similarNode3.getAddr())) {
                        for(IMemNode similarNode2: similar2) {
                            if(similarNode2.getTid() != similarNode1.getTid() && similarNode2.getAddr().equals(similarNode1.getAddr())) {
                                patterns.add(new Pattern(Arrays.asList(similarNode1, similarNode2, similarNode3)));
                            }
                        }

                    }
                }
            }
        } else if(patternNodes.size() == 4) {
            IMemNode node1 = patternNodes.get(0);
            IMemNode node2 = patternNodes.get(1);
            IMemNode node3 = patternNodes.get(2);
            IMemNode node4 = patternNodes.get(3);

            List<IMemNode> similar1 = getAllSimilarNodesLoose(node1, nodes, true);
            List<IMemNode> similar3 = getAllSimilarNodesLoose(node3, nodes, true);
            List<IMemNode> similar2 = getAllSimilarNodesLoose(node2, nodes, true);
            List<IMemNode> similar4 = getAllSimilarNodesLoose(node4, nodes, true);
            Pattern temp = null;
            //1221
            if(node2.getTid() == node3.getTid()) {
                if(node1.getAddr().equals(node2.getAddr())){
                    for(IMemNode similarNode1: similar1) {
                        for(IMemNode similarNode4: similar4) {
                            if(similarNode1.getTid() == similarNode4.getTid() && !similarNode1.getAddr().equals(similarNode4.getAddr())) {
                                for(IMemNode similarNode2: similar2) {
                                    if(similarNode2.getTid() != similarNode1.getTid() && similarNode1.getAddr().equals(similarNode2.getAddr())) {
                                        for(IMemNode similarNode3: similar3) {
                                            if(similarNode2.getTid() == similarNode3.getTid() && similarNode3.getAddr().equals(similarNode4.getAddr())) {
                                                temp = new Pattern(Arrays.asList(node1, node2, node3, node2));
                                                temp.setPatternType(pattern.getPatternType());
                                                patterns.add(temp);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if(node1.getAddr().equals(node3.getAddr())) {
                    for(IMemNode similarNode1: similar1) {
                        for(IMemNode similarNode4: similar4) {
                            if(similarNode1.getTid() == similarNode4.getTid() && !similarNode1.getAddr().equals(similarNode4.getAddr())) {
                                for(IMemNode similarNode2: similar2) {
                                    if(similarNode2.getTid() != similarNode1.getTid() && similarNode2.getAddr().equals(similarNode4.getAddr())) {
                                        for(IMemNode similarNode3: similar3) {
                                            if(similarNode2.getTid() == similarNode3.getTid() && similarNode3.getAddr().equals(similarNode1.getAddr())) {
                                                temp = new Pattern(Arrays.asList(node1, node2, node3, node2));
                                                temp.setPatternType(pattern.getPatternType());
                                                patterns.add(temp);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
//            //1212
            if(node2.getTid() != node3.getTid()) {
                for(IMemNode similarNode1: similar1) {
                    for(IMemNode similarNode3: similar3) {
                        if(similarNode1.getTid() == similarNode3.getTid() && !similarNode1.getAddr().equals(similarNode3.getAddr())) {
                            for(IMemNode similarNode2: similar2) {
                                if(similarNode2.getTid() != similarNode1.getTid() && similarNode2.getAddr().equals(similarNode3.getAddr())) {
                                    for(IMemNode similarNode4: similar4) {
                                        if(similarNode2.getTid() == similarNode4.getTid() && similarNode1.getAddr().equals(similarNode4.getAddr())) {
                                            temp = new Pattern(Arrays.asList(node1, node2, node3, node4));
                                            temp.setPatternType(pattern.getPatternType());
                                            patterns.add(temp);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return patterns;
    }


    public static List<WriteNode> getRelatedWriteNode(List<IMemNode> nodes, Pattern pattern) {
        String addr = pattern.getNodes().get(0).getAddr();
        List<Long> gids = new ArrayList<>();

        for(IMemNode node: pattern.getNodes()) {
            gids.add(node.getGID());
        }


        List<WriteNode> result = nodes.stream().filter(node -> node.getType() == AbstractNode.TYPE.WRITE
                                                       && node.getAddr().equals(addr) && !gids.contains(node.getGID()))
                .map(node -> (WriteNode)node)
                .collect(Collectors.toList());

        return result;
    }
}
