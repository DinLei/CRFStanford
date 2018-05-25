import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BGProBase {
    private final Pattern digit = Pattern.compile("\\d+");
    private Map<String, Map<String, Integer>> concept2instance = null;
    private Map<String, Map<String, Integer>> instance2concept = null;
    private HashSet<String> wordsSet = null;
    private final List<String> preWords = Arrays.asList("for", "with", "in", "on", "of");

    void init() {
        TwoTuple<Map<String, Map<String, Integer>>,
                Map<String, Map<String, Integer>>> proBase = loadProBase();
        this.concept2instance = proBase.getFirst();
        this.instance2concept = proBase.getSecond();
        this.wordsSet = new HashSet<>(instance2concept.keySet());
        this.wordsSet.addAll(this.concept2instance.keySet());
    }

    List<String> getConcepts(String query) {
        query = MyUtil.engStem(query);
        List<String> concepts = new ArrayList<>();
        List<String> components = segment(query, this.wordsSet);
        for (String comp: components) {
            int token_num = comp.split(" ").length;
            String concept = getConcept4singleIns(comp);
            if (token_num < 2)
                concepts.add(concept);
            else {
                while (token_num > 0){
                    concepts.add(concept);
                    token_num -= 1;
                }
            }
        }
        return concepts;
    }

    String getConcept4singleIns(String word) {
        Matcher hasDigit = digit.matcher(word);
        if (hasDigit.find())
            return "#attr#";
        else if (!this.instance2concept.containsKey(word))
            return unkIns(word);
        else {
            String result = null;
            double score = -1;
            Set<String> candiConcepts = this.instance2concept.get(word).keySet();
            for (String concept: candiConcepts) {
                String[] conceptTokens = concept.split(" ");
                String rootConcept = conceptTokens[conceptTokens.length-1];
                double tmpScore = conceptScore4instance(concept, word);
                result = (tmpScore>score)?rootConcept:result;
            }
            return result;
        }
    }

    private String unkIns(String word) {
        if (word.length() == 1)
            return "#char#";
        if (this.preWords.contains(word))
            return "#pre#";
        return "#unk#";
    }

    private double conceptScore4instance(String concept, String instance) {
        if (concept.equals(instance))
            return 1;
        double p_e_by_c = conditionProb(concept, instance, this.concept2instance);
        double p_c_by_e = conditionProb(instance, concept, this.instance2concept);
        return p_e_by_c*p_c_by_e;
    }

    private double conditionProb(String key1, String key2,
                                 Map<String, Map<String, Integer>> dict) {
        if (!dict.containsKey(key1))
            return -1.0;
        Map<String, Integer> area = dict.get(key1);
        if (!area.containsKey(key2))
            return 0.0;
        Double total = (double)area.values().stream().mapToInt(Integer::intValue).sum();
        Integer part = area.get(key2);
        return part/total;
    }

    List<String> segment (String query, Set<String> wordsSet){
        List<String> q_tokens = Arrays.asList(query.split(" "));
        List<String> compo = new ArrayList<>();
        int size = q_tokens.size();
        while (size > 0) {
            String tmp = this.wordsMatch(q_tokens, wordsSet);
            compo.add(0, tmp);
            q_tokens = q_tokens.subList(0, size - (tmp.split(" ").length));
            size = q_tokens.size();
        }
        return compo;
    }

    private String wordsMatch(List<String> tokens, Set<String> wordsSet) {
        while (tokens.size() > 1) {
            String combination = String.join(" ", tokens);
            if (wordsSet.contains(combination)) {
                return combination;
            } else {
                tokens = tokens.subList(1, tokens.size());
            }
        }
        return tokens.get(0);
    }

    private TwoTuple<Map<String, Map<String, Integer>>,
            Map<String, Map<String, Integer>>> loadProBase() {
        Map<String, Map<String, Integer>> ciDict= new HashMap<>();
        Map<String, Map<String, Integer>> icDict= new HashMap<>();
        BufferedReader br;
        String line;
        String cvsSplitBy = ",";
        try {
            br = new BufferedReader(
                    new FileReader(Configs.bgProBaseFile));
            while ((line = br.readLine()) != null) {
                List<String> record =Arrays.asList(line.split(cvsSplitBy));
                String concept = record.get(0).trim();
                String instance = MyUtil.engStem(record.get(1).trim());
                Integer num = Integer.parseInt(record.get(2).trim());
                if(!ciDict.containsKey(concept)){
                    ciDict.put(concept, new HashMap<>());
                }
                if(!icDict.containsKey(instance)){
                    icDict.put(instance, new HashMap<>());
                }
                ciDict.get(concept).put(instance, num);
                icDict.get(instance).put(concept, num);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new TwoTuple<>(ciDict, icDict);
    }

    public Map<String, Map<String, Integer>> getConcept2instance() {
        return concept2instance;
    }

    public Map<String, Map<String, Integer>> getInstance2concept() {
        return instance2concept;
    }


    public static void main(String args[]) {
        BGProBase bgp = new BGProBase();
        String t1 = "naze32 flight controller";
        System.out.println(bgp.getConcepts(t1));
    }
}
