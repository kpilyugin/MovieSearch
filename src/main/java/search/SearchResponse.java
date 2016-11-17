package search;

import lombok.Data;

@Data
public class SearchResponse {
  private final String movieId;
  private final String title;
  private final String plot;
  private final String date;
  private final String poster;
  private final double score;
  private final boolean fromReviews;
  private final String fragment;
}
