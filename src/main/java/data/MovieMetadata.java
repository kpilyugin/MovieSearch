package data;

import lombok.Data;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
1. Wikipedia movie ID
2. Freebase movie ID
3. Movie name
4. Movie release date
5. Movie box office revenue
6. Movie runtime
7. Movie languages (Freebase ID:name tuples)
8. Movie countries (Freebase ID:name tuples)
9. Movie genres (Freebase ID:name tuples)
*/

@Data
public class MovieMetadata {
  public static final String FILE = "movie.metadata.tsv";

  private final int wikiId;
  private final String freebaseId;
  private final String name;
  private final String date;

  public static List<MovieMetadata> collect() {
    List<MovieMetadata> movies = new ArrayList<>();
    try (Scanner scanner = new Scanner(new FileReader(FILE))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String[] values = line.split("\t");
        int wikiId = Integer.parseInt(values[0]);
        movies.add(new MovieMetadata(wikiId, values[1], values[2], values[3]));
      }
      return movies;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
