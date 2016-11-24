package ranking;

import lombok.Data;

import java.util.List;

@Data
public class DatasetEntry {
    private final StackExchangeQuery query;
    private final List<HitResult> results;

    @Data
    public static class StackExchangeQuery {
        private final String post_id;
        private final List<String> imdb;
        private final String title;
        private final String body;
    }
}
