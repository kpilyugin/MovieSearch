package index;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

public class IndexBuilder {

  public static final String INDEX_DIRECTORY = "moviesIndex";
  public static final String DATABASE = "jdbc:postgresql://localhost:5432/imdbcrawl";

  public static void build() throws IOException, SQLException {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory index = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(index, config);
    int counter = 0;

    Connection connection = DriverManager.getConnection(DATABASE, "ir", "");
    connection.setAutoCommit(false);
    System.out.println("Connected to database imdbcrawl");

    Statement statement = connection.createStatement();
    statement.setFetchSize(100);
    ResultSet movie = statement.executeQuery("SELECT * FROM search");
    while (movie.next()) {
      Document document = new Document();
      document.add(new StringField("movie-id", movie.getString("movie_id"), Field.Store.YES));
      document.add(new TextField("plot", movie.getString("plot"), Field.Store.YES));
      document.add(new TextField("reviews", movie.getString("reviews"), Field.Store.YES));
      writer.addDocument(document);

      counter++;
      if (counter % 100 == 0) {
        System.out.println("Processed = " + counter + " movies");
      }
    }

    writer.close();
    connection.close();
  }
}
