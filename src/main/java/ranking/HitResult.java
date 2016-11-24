package ranking;

import lombok.Data;

import java.util.Map;

@Data
public class HitResult {
    private final String id;
    private final Map<String, Double> scores;
}
