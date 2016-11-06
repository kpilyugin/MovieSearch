package data;

import com.google.gson.Gson;
import lombok.Data;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
"m_type": "DVD",
"year": "2007-01-02",
"asin": "B000KWZ2ZG",
"title": "Lovers in Prague",
"genre": "",
"actors": [
    "Kim Joo-Hyuk"
],
"director": ""
 */

@Data
public class AmazonMovie {
  public static final String FILE = "amazon_movies.txt";

  private final String m_type;
  private final String year;
  private final String asin;
  private final String title;
  private final String genre;
  private final String[] actors;
  private final String director;

  public static Map<String, String> collectNameToAsin() {
    try {
      Map<String, String> result = new HashMap<>();
      Gson gson = new Gson();
      AmazonMovie[] movies = gson.fromJson(new FileReader(FILE), AmazonMovie[].class);

      for (AmazonMovie movie : movies) {
        String title = movie.getTitle();
//        System.out.println(title + " - " + movie.getAsin());
        result.put(title, movie.getAsin());
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
