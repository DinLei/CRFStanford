import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.Triple;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @Author: BigDin
 * @Date: 2018/5/12 下午3:46
 * @Email: dinglei_1107@outlook.com
 */
public class HeaderDetect {

    public void trainAndWrite(String modelOutPath, String prop, String trainingFilepath) {
        Properties props = StringUtils.propFileToProperties(prop);
        props.setProperty("serializeTo", modelOutPath);

        if (trainingFilepath != null) {
            props.setProperty("trainFile", trainingFilepath);
        }

        SeqClassifierFlags flags = new SeqClassifierFlags(props);
        CRFClassifier<CoreLabel> crf = new CRFClassifier<>(flags);
        crf.train();

        crf.serializeClassifier(modelOutPath);
    }

    public void doTagging(CRFClassifier model, String input) {
        input = input.trim();
        System.out.println(input + "=>"  +  model.classifyToString(input));
    }

    public CRFClassifier getModel(String modelPath) {
        return CRFClassifier.getClassifierNoExceptions(modelPath);
    }

    public static void main(String[] args) {
        HeaderDetect hd = new HeaderDetect();
        hd.trainAndWrite(
                "re_ner-model.ser.gz",
                "templete.properties",
                "data/re_train4stf.data");
        CRFClassifier model = hd.getModel("re_ner-model.ser.gz");
//        String[] tests = new String[] {
//                "apple watch", "samsung mobile phones", " lcd 52 inch tv"};
//        for (String item : tests) {
//            hd.doTagging(model, item);
//        }
        try {
            DocumentReaderAndWriter readerAndWriter = model.makeReaderAndWriter();

            Triple<Double,Double,Double> out =
                    model.classifyAndWriteAnswers(
                            "data/re_test4stf.data",
                            new FileOutputStream("data/re_out_stf.txt"),
                            readerAndWriter, true);
            System.out.println(out.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
