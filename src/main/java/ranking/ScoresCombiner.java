package ranking;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class ScoresCombiner {
    private static final String coefFile = "/home/ir/src/stackexchange/coef.txt";
    private final HashMap<String, Double> weights;

    public ScoresCombiner() throws IOException {
        weights = new HashMap<>();
        for (String line : FileUtils.readLines(new File(coefFile), Charset.defaultCharset())) {
            String[] res = line.split(" ");
            weights.put(res[0], Double.parseDouble(res[1]));
        }
    }

    public double combine(Map<String, Double> scores) {
        return weights.entrySet().stream()
                .mapToDouble(e -> e.getValue() * scores.getOrDefault(e.getKey(), 0.0))
                .sum();
    }
}
