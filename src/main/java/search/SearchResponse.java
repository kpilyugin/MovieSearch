package search;

import lombok.Data;

@Data
public class SearchResponse {
  private final int wikiId;
  private final String asin;
  private final String name;
  private final String releaseDate;
  private final double score;
  private final boolean fromReviews;
  private final boolean hasReviews;
  private final String fragment;
}
