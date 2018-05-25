import edu.stanford.nlp.ie.crf.CRFClassifier;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class MyUtil {
    static String cleanStr(String query) {
        query = query.replaceAll("[,!?;:\"<>|-]", " ");
        query = query.replaceAll("'s", "");
        query = query.replaceAll("'ve", " have");
        query = query.replaceAll("n't", " not");
        query = query.replaceAll("'re", " are");
        query = query.replaceAll("'d", " would");
        query = query.replaceAll("'ll", " will");
        query = query.replaceAll("\\(", " \\( ");
        query = query.replaceAll("\\)", " \\) ");
        query = query.replaceAll("\\s{2,}", " ");
        return engStem(query.trim().toLowerCase());
    }

    private static int stem(char s[], int len) {
        if (len < 3 || s[len-1] != 's')
            return len;

        switch(s[len-2]) {
            case 'u':
            case 's': return len;
            case 'e':
                if (len > 3 && s[len-3] == 'i' && s[len-4] != 'a' && s[len-4] != 'e') {
                    s[len - 3] = 'y';
                    return len - 2;
                }
                if (s[len-3] == 'i' || s[len-3] == 'a' || s[len-3] == 'o' || s[len-3] == 'e')
                    return len; /* intentional fallthrough */
            default: return len - 1;
        }
    }

    public static String stem(String word) {
        char[] arr = word.toCharArray();
        int end = stem(arr, word.length());
        return new String(arr, 0, end);
    }

    private static String englishMinimalStem(String word) {
        if (word.length() < 3)
            return word;
        Set<String> protectWords = new HashSet<>();
        Map<String, String> specialWords = new HashMap<String, String>(){{
            put("shoes", "shoe");
            put("men", "man"); put("womens", "women");
            put("mens", "man"); put("heroes", "hero");
            put("dresses", "dress");
            put("tomatoes", "tomato"); put("zeroes", "zero");
        }};

        if (protectWords.contains(word))
            return word;
        if (specialWords.containsKey(word))
            return specialWords.get(word);
        return stem(word);
    }

    static String engStem(String query) {
        if (query.length() < 3)
            return query;
        List<String> seq = Arrays.asList(query.split(" "));
        List<String> newSeq = new ArrayList<>();
        seq.forEach(x -> newSeq.add(englishMinimalStem(x)));
        return String.join(" ", newSeq);
    }

    static Set<String> readFile(String filePath) throws FileNotFoundException {
        Set<String> outcome = new HashSet<>();
        BufferedReader br = new BufferedReader(
                new FileReader(filePath));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                outcome.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outcome;
    }

    static <A, B> List<Map.Entry<A, B>> reverseMap(Map<A, B> myMap) {
        List<Map.Entry<A, B>> list = new ArrayList<>(myMap.entrySet());
        list.sort((Map.Entry<A, B> o1, Map.Entry<A, B> o2) -> {
            if (o1 == null) return 1;
            if (o2 == null) return 1;
            String v1 = o1.getValue().toString();
            String v2 = o2.getValue().toString();
            double diff = Double.parseDouble(v2) - Double.parseDouble(v1);
            if (diff > 0) return 1;
            else if (diff == 0) return 0;
            else return -1;
        });
        return list;
    }

    static List<List<String>> readCSV(String filePath) throws IOException {
        BufferedReader br;
        String line;
        String cvsSplitBy = ",";
        List<List<String>> outcome = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(filePath));
            while ((line = br.readLine()) != null) {
                List<String> record =Arrays.asList(line.split(cvsSplitBy));
                List<String> newRecord = new ArrayList<>();
                record.forEach(ele -> newRecord.add(ele.trim().toLowerCase()));
                outcome.add(newRecord);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return outcome;
    }

    static void writeCSV(String filePath, List<List<String>> data) throws IOException {
        try {
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(filePath, true));
            for (List<String> ele: data) {
                bw.write(String.join(",", ele));
                bw.newLine();
            }
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static TwoTuple<List<String>, List<String>> repeatMerge(
            List<String> list1, List<String> list2) {
        assert (list1.size() == list2.size());
        if (list1.size() == 1) {
            return new TwoTuple<>(list1, list2);
        }
        Stack<String> tmpList1 = new Stack<>();
        Stack<String> tmpList2 = new Stack<>();
        Stack<Double> scores = new Stack<>();
        String last = null;

        tmpList1.push(list1.get(0));
        tmpList2.push(list2.get(0));
        scores.push(1.0);
        last = list2.get(0);

        for (int i=1; i<list2.size(); i++) {
            String tmpEle1; String tmpEle2;  Double tmpScore;
            if (list2.get(i).equals(last)) {
                tmpEle1 = tmpList1.pop();
                tmpEle1 += " "+list1.get(i);

                tmpScore = scores.pop();
                tmpScore += 1.0;

                tmpEle2 = tmpList2.pop();
            } else {
                tmpEle1 = list1.get(i);
                tmpEle2 = list2.get(i);
                tmpScore = 1.0;
            }
            tmpList1.push(tmpEle1);
            tmpList2.push(tmpEle2);
            scores.push(tmpScore);
            last = tmpEle2;
        }
        List<String> tmpList1_copy = new ArrayList<>(tmpList1);
        List<String> tmpList2_copy = new ArrayList<>(tmpList2);
        if (tmpList2.size() != new HashSet<>(tmpList2).size()) {
            Map<String, List<Integer>> tmpCounter = counter(tmpList2);
            for (Map.Entry<String, List<Integer>> me: tmpCounter.entrySet()) {
                if (me.getValue().size() > 1) {
                    List<Integer> repeatIds = me.getValue();
                    List<Double> tmpScores = subList(scores, repeatIds);
                    Double maxScore = Collections.max(tmpScores);
                    repeatIds.remove(tmpScores.lastIndexOf(maxScore));
                    for (Integer id: repeatIds) {
                        tmpList1_copy.remove(id.intValue());
                        tmpList2_copy.remove(id.intValue());
                    }
                }
            }
        }
        return new TwoTuple<>(tmpList1_copy, tmpList2_copy);
    }

    static <T> Map<T, List<Integer>> counter(List<T> list) {
        Map<T, List<Integer>> myMap = new HashMap<>();
        for (int i=0; i<list.size(); i++) {
            T ele = list.get(i);
            if (!myMap.containsKey(ele))
                myMap.put(ele, new ArrayList<>());
            myMap.get(ele).add(i);
        }
        return myMap;
    }

    static <T> List<T> subList(List<T> list, List<Integer> ids) {
        List<T> tmpList = new ArrayList<>();
        for (Integer i: ids) {
            tmpList.add(list.get(i));
        }
        return tmpList;
    }

    static <T> List<Integer> subListInds(List<T> list, List<T> seq) {
        Integer start = -1;
        Integer end = -1;
        for (int i=0; i<list.size(); i++) {
            if (start == -1 && list.get(i).equals(seq.get(0)))
                start = i;
            if (start > -1 && list.get(i).equals(seq.get(seq.size()-1)))
                end = i+1;
        }
        return IntStream.range(start, end).boxed().collect(Collectors.toList());
    }

    static Double sum(Collection<Integer> values) {
        Double out = 0.0;
        for (Integer x: values)
            out += x;
        return out;
    }

    static void validate4crf(String validFile, String sep, CRFClassifier model)
            throws FileNotFoundException {
        BufferedReader br = new BufferedReader(
                new FileReader(validFile));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String row = line.trim().toLowerCase();
                String[] tokens = row.split(sep);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class TwoTuple<A, B> {
    private final A first;
    private final B second;

    TwoTuple(A a, B b) {
        this.first = a;
        this.second = b;
    }

    A getFirst() {
        return first;
    }

    B getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "TwoTuple{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}


class ThreeTuple<A, B, C> {
    private final A first;
    private final B second;
    private final C third;

    ThreeTuple(A a, B b, C c) {
        this.first = a;
        this.second = b;
        this.third = c;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public C getThird() {
        return third;
    }

    @Override
    public String toString() {
        return "ThreeTuple{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }
}