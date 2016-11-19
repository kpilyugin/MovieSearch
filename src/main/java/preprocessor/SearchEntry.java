package preprocessor;

import lombok.Data;

@Data
public class SearchEntry {
    private final String movie_id;
    private final String plot;
    private final String reviews;
    private String preprocessed_plot;
    private String preprocessed_reviews;
}
