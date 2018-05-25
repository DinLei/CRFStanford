import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Prepare {

    static final Pattern tagReg = Pattern.compile("\\[(?:@|\\$)(.*)#(.*)\\*");

    static void trainDataReform(
            String fileName, String saveFile,
            String sep, boolean addConcept) {
        BGProBase bgp = new BGProBase();
        if (addConcept) {
            bgp.init();
        }
        BufferedReader reader;
        BufferedWriter output;
        String line;
        List<String> labels = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        try {
            reader = new BufferedReader(
                    new FileReader(fileName));
            output = new BufferedWriter(
                    new FileWriter(saveFile, true));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) {
                    if (addConcept) {
                        String sentence = String.join(" ", tokens);
                        List<String> concepts = bgp.getConcepts(sentence.trim());
                        for (int i=0; i<tokens.size(); i++) {
                            output.write(tokens.get(i) + sep +
                                    labels.get(i) + sep + concepts.get(i));
                            output.newLine();
                        }
                        output.write("\n");
                    } else {
                        for (int i=0; i<tokens.size(); i++) {
                            output.write(tokens.get(i) + sep + labels.get(i));
                            output.newLine();
                        }
                        output.write("\n");
                    }
                    labels.clear(); tokens.clear();
                } else {
                    String[] ele = line.split("\\s+|\t+");
                    labels.add(ele[1]);
                    tokens.add(ele[0]);
                }
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static List<String> segment(String chars) {
        List<String> tokens = new ArrayList<>();
        chars = chars.replaceAll("]\\[", "] \\[");
        chars = chars.replaceAll("\\s+", " ").trim();
        List<Character> tmpChars = new ArrayList<>();
        boolean leftBound = false;
        for (char ch: chars.toCharArray()) {
            if (ch == '[') leftBound=true;
            if (ch == ']') leftBound=false;
            if (ch == ' ' && !leftBound) {
                tokens.add(
                        tmpChars.stream().map(e->e.toString()).collect(Collectors.joining()));
                tmpChars.clear();
            } else {
                tmpChars.add(ch);
            }
        }
        if (!tmpChars.isEmpty()) {
            tokens.add(
                    tmpChars.stream().map(e->e.toString()).collect(Collectors.joining()));
        }
        return tokens;
    }

    static void trainDataGen(String fromDir, String toFile, String sep,
                             String hLabel, String mLabel) throws IOException {
        File file = new File(fromDir);
        File[] tempList = file.listFiles();
        BufferedReader reader;
        BufferedWriter output = new BufferedWriter(new FileWriter(toFile));
        for (int i = 0; i < tempList.length; i++) {
            reader = new BufferedReader(new FileReader(tempList[i]));
            String line; String label;
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase().trim();
                    if (line.equals("")) continue;
                    List<String> tokens = segment(line);
                    for (String ti: tokens) {
                        Matcher matcher = tagReg.matcher(ti);
                        if (matcher.find()) {
                            ti = matcher.group(1).trim();
                            label = hLabel;
                        } else {
                            label = mLabel;
                        }
                        for (String tti: ti.split(" ")) {
                            output.write(MyUtil.stem(tti) + sep + label);
                            output.newLine();
                        }
                    }
                    output.write("\n");
                }
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            trainDataGen("data/test_data", "data/test.data",
                    "\t", "c", "m");
            trainDataGen("data/train_data", "data/train.data",
                    "\t", "c", "m");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
