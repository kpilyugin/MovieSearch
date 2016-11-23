package ranking;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class WordStatsCalculator {
    //private static final String wordRegex = "[a-z-]+";
    //private static final Pattern wordRegexPattern = Pattern.compile(wordRegex);
    private static final String stopWordsFile = "/home/ir/raw/stopwords.txt";
    private final HashSet<String> stopWords;
    private final StanfordCoreNLP lemmatizerPipeline;

    public WordStatsCalculator() throws IOException {
        stopWords = new HashSet<>(FileUtils.readLines(new File(stopWordsFile), Charset.defaultCharset()));
        final Properties openieProps = new Properties();
        openieProps.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        lemmatizerPipeline = new StanfordCoreNLP(openieProps);
    }

    public void process(Map<String, WordStat> stats, String text) {
        try {
            final Annotation doc = new Annotation(text);
            lemmatizerPipeline.annotate(doc);

            final HashMap<String, Integer> counter = new HashMap<>();
            final List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
            for(CoreMap sentence: sentences) {
                for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    final String lemma = token.get(CoreAnnotations.LemmaAnnotation.class).toLowerCase();
                    if (!stopWordsContains(lemma)) {
                        counter.put(lemma, counter.getOrDefault(lemma, 0) + 1);
                    }
                }
            }
            for (Map.Entry<String, Integer> entry : counter.entrySet()) {
                WordStat stat = stats.get(entry.getKey());
                if (stat == null) {
                    stat = new WordStat();
                    stat.cf = 0;
                    stat.df = 0;
                }
                stat.df++;
                stat.cf += entry.getValue();
                stats.put(entry.getKey(), stat);
            }
        } catch (Exception e) {
            System.err.println("failed to process: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    boolean stopWordsContains(String word) {
        //return !wordRegexPattern.matcher(word).matches() || stopWords.contains(word);
        return stopWords.contains(word);
    }

    @Data
    public static class WordStat {
        private long cf;
        private long df;
    }
}
