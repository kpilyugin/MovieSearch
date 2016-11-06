package data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class AmazonReviewsOld {
  public static final String REVIEWS_FILE = "reviews/reviews.json";
  public static final String DB_FILE = "reviews/reviews.db";

  private final DB db = DBMaker.fileDB(DB_FILE).make();
  @SuppressWarnings("unchecked")
  private final Map<String, List<String>> asinToReviews = (Map<String, List<String>>) db.hashMap("map").createOrOpen();

  public String getReviews(String asin) {
    List<String> reviews = asinToReviews.get(asin);
    return reviews != null ? StringUtils.join(reviews, " ") : null;
  }

  public Set<String> asins() {
    return asinToReviews.keySet();
  }

  public void listAll() {
    asinToReviews.keySet().forEach(System.out::println);
  }

  public void closeDb() {
    db.close();
  }

  public static void collectReviews() {
    DB db = DBMaker.fileDB(DB_FILE).make();
    //noinspection unchecked
    Map<String, List<String>> map = (Map<String, List<String>>) db.hashMap("map").createOrOpen();

    int counter = 0;
    try (Scanner scanner = new Scanner(new FileReader(REVIEWS_FILE))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
        counter++;
        if (counter % 1000 == 0) {
          System.out.println("counter = " + counter);
        }
        String id = jsonObject.get("asin").getAsString();
        List<String> reviews = map.containsKey(id) ? new ArrayList<>(map.get(id)) : new ArrayList<>();
        reviews.add(jsonObject.get("reviewText").getAsString());
        map.put(id, reviews);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("map.size() = " + map.size());
    db.close();
  }
}
