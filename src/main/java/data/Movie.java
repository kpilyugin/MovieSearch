package data;

import lombok.Data;

@Data
public class Movie {
  private final int wikiId;
  private final String asin;
  private final String name;
  private final String summary;
  private final String reviews;
  private final String date;
}
