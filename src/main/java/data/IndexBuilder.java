package data;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class IndexBuilder {

  public static final String INDEX_DIRECTORY = "moviesIndex";

  public static void build() throws IOException {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory index = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(index, config);
    int counter = 0;

    List<Movie> movies = DataCollector.collect();
    for (Movie movie : movies) {
      counter++;
      if (counter % 1000 == 0) {
        System.out.println("Processed = " + counter + " movies");
      }
      Document document = new Document();
      document.add(new IntField("wikiId", movie.getWikiId(), Field.Store.YES));
      document.add(new StringField("asin", nonNull(movie.getAsin()), Field.Store.YES));
      document.add(new StringField("name", movie.getName(), Field.Store.YES));
      document.add(new StringField("date", movie.getDate(), Field.Store.YES));
      document.add(new TextField("summary", nonNull(movie.getSummary()), Field.Store.YES));
      document.add(new TextField("reviews", nonNull(movie.getReviews()), Field.Store.YES));
      writer.addDocument(document);
    }
    writer.close();
  }

  private static String nonNull(String value) {
    return value != null ? value : "";
  }
}
