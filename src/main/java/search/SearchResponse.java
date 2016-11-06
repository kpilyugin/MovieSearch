package search;

import lombok.Data;

@Data
public class SearchResponse {
  private final int wikiId;
  private final String name;
  private final double score;
}
