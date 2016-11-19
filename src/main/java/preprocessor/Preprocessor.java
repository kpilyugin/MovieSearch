package preprocessor;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations.RelationTriplesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.wordnet.SynonymMap;

import java.io.*;
import java.util.*;

public class Preprocessor {
    private static final String wordNetPath = "/home/ir/data/wordnet/prolog/wn_s.pl";
    private static final double confidenceLevel = 0.95;
    private static final int synonymCount = 3;
    private static final String emptyResponse = " ";

    private final SynonymMap synonymMap;
    private final StanfordCoreNLP openiePipeline;

    public Preprocessor() throws IOException {
        synonymMap = new SynonymMap(new FileInputStream(wordNetPath));
        final Properties openieProps = new Properties();
        openieProps.setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
        openiePipeline = new StanfordCoreNLP(openieProps);
    }

    public String process(String text) {
        try {
            text = fix(text);
            final Annotation doc = new Annotation(text);
            openiePipeline.annotate(doc);

            String result = "";
            for (CoreMap sentence : doc.get(SentencesAnnotation.class)) {
                if (sentence.size() > 1000) {
                    System.err.println("too long sentence");
                    return emptyResponse;
                }

                final Collection<RelationTriple> triples =
                        sentence.get(RelationTriplesAnnotation.class);
                RelationTriple best = null;
                for (RelationTriple triple : triples) {
                    if (triple.confidence < confidenceLevel) continue;
                    if (best == null || triple.allTokens().size() > best.allTokens().size()) {
                        best = triple;
                    }
                    //System.out.println(triple.toString());
                }
                final List<String> words = new ArrayList<>();
                if (best != null) {
                    for (CoreLabel label : best.object) {
                        words.addAll(getSynonyms(label));
                    }
                    for (CoreLabel label : best.relation) {
                        words.addAll(getSynonyms(label));
                    }
                    for (CoreLabel label : best.subject) {
                        words.addAll(getSynonyms(label));
                    }
                }
                result += String.join(" ", words) + ". ";
            }
            return result;
        } catch (Exception e) {
            System.err.println("failed to process: " + e.getMessage());
            e.printStackTrace(System.err);
            return emptyResponse;
        }
    }

    private static String fix(String plot) {
        if (plot.startsWith("{{plot}}")) {
            return plot.substring(8);
        }
        return plot;
    }

    private List<String> getSynonyms(CoreLabel token) {
        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        List<String> synonyms = Arrays.asList(synonymMap.getSynonyms(lemma));
        synonyms = new ArrayList<>(synonyms);
        synonyms.add(0, lemma);
        if (synonyms.size() > synonymCount)
            synonyms.subList(synonymCount, synonyms.size()).clear();
        return synonyms;
    }
}
