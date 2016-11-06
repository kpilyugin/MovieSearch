package data;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PlotSummary {
  public static final String FILE = "plot_summaries.txt";

  public static Map<Integer, String> collectSummaries() {
    try (Scanner scanner = new Scanner(new FileReader(FILE))) {
      Map<Integer, String> result = new HashMap<>();
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String values[] = line.split("\t", 2);
        result.put(Integer.parseInt(values[0]), values[1]);
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
