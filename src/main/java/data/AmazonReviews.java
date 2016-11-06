package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AmazonReviews {
  private static final String FILE = "amazon_reviews.txt";

  public static Map<String, String> collectReviews(Set<String> asins) {
    Map<String, String> result = new HashMap<>();
    int count = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
      String line;
      String currentAsin = null;
      while ((line = reader.readLine()) != null) {
        count++;
        if (count % 1e6 == 0) {
          System.out.println("Processed " + count + " reviews.");
        }
        String[] values = line.split(" ", 2);
        if (values[0].contains("productId")) {
          if (asins.contains(values[1])) {
            currentAsin = values[1];
          }
        } else if (values[0].contains("review/text")) {
          if (currentAsin != null) {
            String previous = result.getOrDefault(currentAsin, "");
            result.put(currentAsin, previous + values[1]);
            currentAsin = null;
          }
        }
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
