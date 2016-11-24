package ranking;

import ranking.Ranking.QueryInfo;
import ranking.Ranking.SearchCandidate;
import ranking.WordStatsCalculator.WordStat;
import search.DbConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
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

    public static final String[] SCORES = {
            "lucene",
            "review_cnt",
            "cos-p", "tf-idf_p", "bm25_p",
            "cos-pp", "tf-idf_pp", "bm25_pp",
            "has_plot", "has_pplot",
            "substr_plot", "substr_review",
            "uword_p", "uword_pp",
            "fw_p", "fw_pp"
    };

    private final Preprocessor preprocessor;

    public ScoresCalculator(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public Map<String, Double> calculateScores(SearchCandidate candidate, QueryInfo queryInfo) {
        Map<String, Double> scores = new HashMap<>();
        scores.put("id", 1.0);
        scores.put("lucene", candidate.getLuceneScore());
        scores.put("review_cnt", (double) candidate.getReviewCount());
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
        scores.put("has_plot", candidate.getPlotStats().isEmpty() ? 0.0 : 1.0);
        scores.put("has_pplot", candidate.getProcessedPlotsStats().isEmpty() ? 0.0 : 1.0);
        scores.put("substr_plot", hasSubstring(queryInfo.getQuery(), candidate.getPlot()));
        scores.put("substr_review", hasSubstring(queryInfo.getQuery(), candidate.getReviews()));
        scores.put("fw_p", foundWordsFraction(queryInfo.getStats(), candidate.getPlotStats()));
        scores.put("fw_pp", foundWordsFraction(queryInfo.getStats(), candidate.getProcessedPlotsStats()));
        return scores;
    }

    public void calculateComplexScores(List<SearchCandidate> candidates, QueryInfo queryInfo) {
        final Map<SearchCandidate, Double> uniquePlotWords = new HashMap<>();
        final Map<SearchCandidate, Double> uniquePPlotWords = new HashMap<>();
        for (String word : queryInfo.getStats().keySet()) {
            SearchCandidate uniquePlotCandidate = null;
            boolean uniquePlot = true;
            for (SearchCandidate candidate : candidates) {
                if (candidate.getPlotStats().containsKey(word)) {
                    if (uniquePlotCandidate == null) {
                        uniquePlotCandidate = candidate;
                    } else {
                        uniquePlot = false;
                        break;
                    }
                }
            }
            if (uniquePlot) {
                uniquePlotWords.put(uniquePlotCandidate,
                        1.0 + uniquePlotWords.getOrDefault(uniquePlotCandidate, 0.0));
            }
            SearchCandidate uniquePPlotCandidate = null;
            boolean uniquePPlot = true;
            for (SearchCandidate candidate : candidates) {
                if (candidate.getProcessedPlotsStats().containsKey(word)) {
                    if (uniquePPlotCandidate == null) {
                        uniquePPlotCandidate = candidate;
                    } else {
                        uniquePPlot = false;
                        break;
                    }
                }
            }
            if (uniquePPlot) {
                uniquePPlotWords.put(uniquePPlotCandidate,
                        1.0 + uniquePPlotWords.getOrDefault(uniquePPlotCandidate, 0.0));
            }
        }
        int words = queryInfo.getStats().size();
        uniquePlotWords.entrySet()
                .forEach(e -> e.getKey().getScores().put("uword_p", e.getValue() / words));
        uniquePPlotWords.entrySet()
                .forEach(e -> e.getKey().getScores().put("uword_pp", e.getValue() / words));
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

    private static double hasSubstring(String query, String document) {
        return document.toLowerCase().contains(query.toLowerCase()) ? 1.0 : 0.0;
    }

    private static double foundWordsFraction(Map<String, WordStat> query,
                                             Map<String, WordStat> document) {
        double foundWords = 0;
        for (String word : query.keySet()) {
            if (document.containsKey(word)) {
                foundWords++;
            }
        }
        return foundWords / query.size();
    }
}
