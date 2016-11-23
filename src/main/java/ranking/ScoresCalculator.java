package ranking;

import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import ranking.WordStatsCalculator.WordStat;

import java.util.HashMap;
import java.util.Map;

public class ScoresCalculator {
    private static final double BM25_K1 = 1.3;
    private static final double BM25_B = 0.75;
    //select count(*) from search;
    private static final double N = 115150;
    //select AVG(char_length(plot)) from search;
    private static final double PLOT_DL_AVE = 449.0351020408163265;
    //select AVG(char_length(preprocessed_plot)) from search;
    private static final double PPLOT_DL_AVE = 304.5917151541467651;

    private final Preprocessor preprocessor;

    public ScoresCalculator(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public Map<String, Double> calculateScores(SearchCandidate candidate, QueryInfo queryInfo) {
        Map<String, Double> scores = new HashMap<>();
        scores.put("id", 1.0);
        scores.put("lucene", candidate.getLuceneScore());
        if (!candidate.getPlotStats().isEmpty()) {
            scores.put("cos_p", cosScore(queryInfo.getStats(), candidate.getPlotStats()));
            scores.put("tf-idf_p", TFIDF(queryInfo.getStats(), candidate.getPlotStats(), preprocessor.plotsStat));
            scores.put("bm25_p", BM25(queryInfo.getStats(), candidate.getPlotStats(), preprocessor.plotsStat, PLOT_DL_AVE));
        }
        if (!candidate.getProcessedPlotsStats().isEmpty()) {
            scores.put("cos_pp", cosScore(queryInfo.getStats(), candidate.getProcessedPlotsStats()));
            scores.put("tf-idf_pp", TFIDF(queryInfo.getStats(), candidate.getProcessedPlotsStats(), preprocessor.processedPlotsStat));
            scores.put("bm25_pp", BM25(queryInfo.getStats(), candidate.getProcessedPlotsStats(), preprocessor.processedPlotsStat, PPLOT_DL_AVE));
        }
        return scores;
    }

    private static double sqr(double d) {
        return d * d;
    }

    private static double cosNorm(Map<String, WordStat> stats) {
        return Math.sqrt(stats.values().stream().mapToDouble(d -> sqr(d.getCf())).sum());
    }

    private double cosScore(Map<String, WordStat> query,
                            Map<String, WordStat> document) {
        double score = 0.0;
        for (String word : query.keySet()) {
            if (!document.containsKey(word)) continue;
            double queryCount = query.get(word).getCf();
            double documentCount = document.get(word).getCf();
            score += queryCount * documentCount;
        }
        return score
                / cosNorm(query)
                / cosNorm(document);
    }

    private static double idf_1(String word, Map<String, WordStat> collection) {
        if (!collection.containsKey(word)) return 0.0;
        double df = collection.get(word).getDf();
        //return Math.log(N / df);
        return Math.max(0.0, Math.log((N - df) / df));
    }

    private static double tfidfNorm(Map<String, WordStat> stats, Map<String, WordStat> collection) {
        return Math.sqrt(stats.entrySet().stream()
                .mapToDouble(e -> sqr(e.getValue().getCf() * idf_1(e.getKey(), collection)))
                .sum());
    }

    private double TFIDF(Map<String, WordStat> query,
                         Map<String, WordStat> document,
                         Map<String, WordStat> collection) {
        double score = 0.0;
        for (String word : query.keySet()) {
            if (!document.containsKey(word)) continue;
            if (!collection.containsKey(word)) continue;
            double queryCount = query.get(word).getCf();
            double documentCount = document.get(word).getCf();
            queryCount *= idf_1(word, collection);
            documentCount *= idf_1(word, collection);
            score += queryCount * documentCount;
        }
        return score
                / tfidfNorm(query, collection)
                / tfidfNorm(document, collection);
    }

    private static double idf_2(String word, Map<String, WordStat> collection) {
        if (!collection.containsKey(word)) return 0.0;
        double df = collection.get(word).getDf();
        return Math.log(N / df);
        //return Math.max(0.0, Math.log((N - df) / df));
        //return Math.max(0.0, Math.log((N - df + 0.5) / (df + 0.5)));
    }

    private double BM25(Map<String, WordStat> query,
                        Map<String, WordStat> document,
                        Map<String, WordStat> collection,
                        double dl_ave) {
        double score = 0.0;
        double dl = document.values().stream().map(WordStat::getCf).mapToDouble(d -> d).sum();
        for (String word : query.keySet()) {
            if (!document.containsKey(word)) continue;
            if (!collection.containsKey(word)) continue;
            double idf = idf_2(word, collection);
            double tf = document.get(word).getCf();
            score += idf * (tf * (BM25_K1 + 1)) / (tf + BM25_K1 * ((1 - BM25_B) + BM25_B * dl / dl_ave));
        }
        return score / query.size();
    }
}
