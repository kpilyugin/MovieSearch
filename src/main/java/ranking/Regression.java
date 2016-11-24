package ranking;

import com.google.gson.Gson;
import lombok.Data;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ranking.ScoresCalculator.SCORES;

public class Regression {

    private static final String WIMM = "/home/ir/src/stackexchange/wimm_ranged.json";

    private final DatasetEntry[] dataset = readDataset();
    private final WimmRanking[] wimmRankings = readWimmRanking();
    private final LinearRegression model = new LinearRegression();
    private final Map<DatasetEntry, Map<HitResult, Instance>> trainingSet = new HashMap<>();

    public Regression() throws IOException {
    }

    public static void calculateCoefs() {
        try {
            Regression regression = new Regression();
            regression.trainModel();
//            regression.writeCoefs();

            regression.evalLuceneRanking();
            regression.evalRegressionRanking();
            regression.evalWimmRanking();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static DatasetEntry[] readDataset() throws IOException {
        try (FileReader reader = new FileReader(DatasetBuilder.resultsPath)) {
            return new Gson().fromJson(reader, DatasetEntry[].class);
        }
    }

    private static WimmRanking[] readWimmRanking() throws IOException {
        try (FileReader reader = new FileReader(WIMM)) {
            return new Gson().fromJson(reader, WimmRanking[].class);
        }
    }

    private void writeCoefs() throws IOException {
        double[] coefs = model.coefficients();
        try (FileWriter writer = new FileWriter(ScoresCombiner.COEF_FILE)) {
            for (int i = 0; i < SCORES.length; i++) {
                if (coefs[i] != 0) {
                    writer.write(SCORES[i] + " " + coefs[i] + "\n");
                }
            }
        }
    }

    private LinearRegression trainModel() throws Exception {
        model.setRidge(100);

        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < SCORES.length; i++) {
            attributes.add(new Attribute(SCORES[i], i));
        }
        Attribute classAttr = new Attribute("Class", SCORES.length);
        attributes.add(classAttr);

        Instances instances = new Instances("train", attributes, 1000);
        instances.setClass(classAttr);

        for (DatasetEntry entry : dataset) {
            List<String> answerImdb = entry.getQuery().getImdb();
            Map<HitResult, Instance> queryInstances = new HashMap<>();
            for (HitResult hitResult : entry.getResults()) {
                boolean correct = answerImdb.contains(hitResult.getId());
                DenseInstance instance = new DenseInstance(SCORES.length + 1);
                instance.setDataset(instances);
                for (int i = 0; i < SCORES.length; i++) {
                    double value = hitResult.getScores().getOrDefault(SCORES[i], 0d);
                    instance.setValue(i, value);
                }
                instance.setClassValue(correct ? 10 : 0);
                instances.add(instance);
                queryInstances.put(hitResult, instance);
            }
            trainingSet.put(entry, queryInstances);
        }
        System.out.println("Training model...");
        System.out.println("Train instances size = " + instances.size());
        model.setDebug(true);
        model.setOutputAdditionalStats(true);
        model.buildClassifier(instances);
        System.out.println("Result regression: " + model);
        return model;
    }

    private void evalLuceneRanking() {
        RankingStats stats = new RankingStats();
        for (DatasetEntry entry : dataset) {
            List<String> answerImdb = entry.getQuery().getImdb();
            if (!answerImdb.isEmpty()) {
                List<String> ranks = entry.getResults().stream()
                        .sorted(Comparator.comparingDouble(info -> -info.getScores().get("lucene")))
                        .map(HitResult::getId)
                        .collect(Collectors.toList());
                int answerRank = ranks.indexOf(answerImdb.get(0));
                stats.add(answerRank);
            }
        }
        System.out.println("Lucene ranking: " + stats);
    }

    private void evalRegressionRanking() {
        RankingStats stats = new RankingStats();
        trainingSet.forEach((entry, value) -> {
            List<String> answerImdb = entry.getQuery().getImdb();
            if (!answerImdb.isEmpty()) {
                List<RankingInfo> ranking = new ArrayList<>();
                value.forEach((result, instance) -> {
                    try {
                        ranking.add(new RankingInfo(result.getId(), model.classifyInstance(instance)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                List<String> ranks = ranking.stream()
                        .sorted(Comparator.comparingDouble(info -> -info.getScore()))
                        .map(RankingInfo::getId)
                        .collect(Collectors.toList());
                int answerRank = ranks.indexOf(answerImdb.get(0));
                stats.add(answerRank);
            }
        });
        System.out.println("Regression ranking: " + stats);
    }

    private void evalWimmRanking() {
        RankingStats stats = new RankingStats();

        for (WimmRanking ranking : wimmRankings) {
            if (ranking.getAnswer().isEmpty()) {
                continue;
            }
            stats.add(ranking.answerRank());
        }
        System.out.println("Wimm ranking: " + stats);
    }

    @Data
    private static class RankingStats {
        private int withAnswer;
        private int noMatch;
        private int outOfTen;
        private final int[] rankings = new int[10];

        public void add(int answerRank) {
            withAnswer++;
            if (answerRank == -1) {
                noMatch++;
            } else if (answerRank >= 10) {
                outOfTen++;
            } else {
                rankings[answerRank]++;
            }
        }
    }

    @Data
    private static class RankingInfo {
        private final String id;
        private final double score;
    }

    @Data
    private static class WimmRanking {
        private final String query;
        private final List<String> answer;
        private final List<String> wimm;

        private int answerRank() {
            return wimm.indexOf(answer.get(0));
        }
    }
}
