package data;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DataCollector {

  public static List<Movie> collect() {
    Map<String, String> nameToAsin = AmazonMovie.collectNameToAsin();
    System.out.println("Title to asin map = " + nameToAsin.size());

    Map<Integer, String> summaries = PlotSummary.collectSummaries();
    System.out.println("Number of summaries = " + summaries.size());
    List<MovieMetadata> movies = MovieMetadata.collect();
    System.out.println("Number of movies with metadata = " + movies.size());

    Set<String> usedAsins = new HashSet<>();
    for (MovieMetadata movie : movies) {
      String asin = nameToAsin.get(movie.getName());
      if (asin != null) {
        usedAsins.add(asin);
      }
    }
    System.out.println("usedAsins.size() = " + usedAsins.size());
    Map<String, String> asinToReviews = AmazonReviews.collectReviews(usedAsins);
    Set<String> nameAsins = new HashSet<>(nameToAsin.values());
    System.out.println("nameAsins.size() = " + nameAsins.size());

    List<Movie> resultMovies = new ArrayList<>();
    for (MovieMetadata movie : movies) {
      String summary = summaries.get(movie.getWikiId());
      String asin = nameToAsin.get(movie.getName());
      String review = asinToReviews.get(asin);
      if (summary != null || review != null) {
        resultMovies.add(new Movie(movie.getWikiId(), asin, movie.getName(), summary, review, movie.getDate()));
      }
    }
    return resultMovies;
  }

  public static void main(String[] args) throws IOException {
    List<Movie> movies = collect();
    System.out.println("movies.size() = " + movies.size());
    int withAsins = 0;
    int withReviews = 0;
    try (FileWriter writer = new FileWriter("movie_list.txt")) {
      for (Movie movie : movies) {
        if (movie.getAsin() != null) {
          withAsins++;
        }
        boolean hasReviews = movie.getReviews() != null;
        if (hasReviews) {
          withReviews++;
        }
        writer.write(movie.getWikiId() + ": " + movie.getName() + ": asin = " + movie.getAsin() + ", reviews: " + hasReviews + "\n");
      }
    }
    System.out.println("withAsins = " + withAsins);
    System.out.println("withReviews = " + withReviews);
  }
}
