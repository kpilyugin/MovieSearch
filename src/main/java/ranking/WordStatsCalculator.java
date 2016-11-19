package ranking;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import lombok.Data;

import java.io.IOException;
import java.util.*;

public class WordStatsCalculator {
    private final StanfordCoreNLP lemmatizerPipeline;

    public WordStatsCalculator() throws IOException {
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
                    final String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                    counter.put(lemma, counter.getOrDefault(lemma, 0) + 1);
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

    @Data
    public static class WordStat {
        private long cf;
        private long df;
    }
}
