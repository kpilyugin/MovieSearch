package ranking;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import lombok.Data;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

import static ranking.ScoresCalculator.SCORES;

public class Regression {

    private static final String WIMM = "/home/ir/src/stackexchange/wimm_ranged.json";

    private final DatasetEntry[] dataset;
    private final WimmRanking[] wimmRankings;
    private final LinearRegression model = new LinearRegression();
    private final Set<Integer> testIndices = new HashSet<>();
    private final Map<DatasetEntry, Map<HitResult, Instance>> testSet = new HashMap<>();

    public Regression() throws IOException {
        dataset = readDataset();
        wimmRankings = readWimmRanking();
    }

    public static void calculateCoefs() {
        try {
            Regression regression = new Regression();
            regression.trainModel();
            regression.writeCoefs();

            regression.evalLuceneRanking();
            regression.evalRegressionRanking();
            regression.evalWimmRanking();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static DatasetEntry[] readDataset() throws IOException {
        List<DatasetEntry> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(DatasetBuilder.resultsPath));
            JsonReader jsonReader = new JsonReader(reader)) {
            Gson gson = new Gson();
            jsonReader.beginArray();
            int count = 0;
            while (jsonReader.hasNext()) {
                DatasetEntry entry = gson.fromJson(jsonReader, DatasetEntry.class);
                count++;
                if (count % 100 == 0) {
                    System.out.println("read = " + count);
                }
                result.add(entry);
            }
            return result.toArray(new DatasetEntry[0]);
        }
    }

    private static WimmRanking[] readWimmRanking() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(WIMM))) {
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
        System.out.println("Building model...");
        model.setRidge(100);

        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < SCORES.length; i++) {
            attributes.add(new Attribute(SCORES[i], i));
        }
        Attribute classAttr = new Attribute("Class", SCORES.length);
        attributes.add(classAttr);

        Instances train = new Instances("train", attributes, 1000);
        train.setClass(classAttr);

        Random r = new Random();
        while (testIndices.size() < dataset.length * 0.2) {
            testIndices.add(r.nextInt(dataset.length));
        }
        for (int i = 0; i < dataset.length; i++) {
            DatasetEntry entry = dataset[i];
            List<String> answerImdb = entry.getQuery().getImdb();
            Map<HitResult, Instance> queryInstances = new HashMap<>();
            for (HitResult hitResult : entry.getResults()) {
                boolean correct = answerImdb.contains(hitResult.getId());
                DenseInstance instance = new DenseInstance(SCORES.length + 1);
                instance.setDataset(train);
                for (int j = 0; j < SCORES.length; j++) {
                    double value = hitResult.getScores().getOrDefault(SCORES[j], 0d);
                    instance.setValue(j, value);
                }
                instance.setClassValue(correct ? 10 : 0);
                if (!testIndices.contains(i)) {
                    train.add(instance);
                } else {
                    queryInstances.put(hitResult, instance);
                }
            }
            if (testIndices.contains(i)) {
                testSet.put(entry, queryInstances);
            }
        }
        System.out.println("Training model...");
        int testSize = testIndices.size();
        System.out.println("Train size = " + (dataset.length - testSize) + ", test size = " + testSize);
        model.setDebug(true);
        model.setOutputAdditionalStats(true);
        model.buildClassifier(train);
        System.out.println("Result regression: " + model);
        return model;
    }

    private void evalLuceneRanking() {
        RankingStats stats = new RankingStats();
        for (DatasetEntry entry : subset(dataset, testIndices)) {
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
        testSet.forEach((entry, value) -> {
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

        for (WimmRanking ranking : subset(wimmRankings, testIndices)) {
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

    private static <T> T[] subset(T[] data, Set<Integer> indices) {
        //noinspection unchecked
        T[] result = (T[]) Array.newInstance(data[0].getClass(), indices.size());
        int i = 0;
        for (Integer idx : indices) {
            result[i++] = data[idx];
        }
        return result;
    }
}
