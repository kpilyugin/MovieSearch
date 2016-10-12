package ru.spbau.mit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

public class ReviewsIndex {

  public static final String INDEX_DIRECTORY = "reviewsIndex";

  public static void build() throws IOException {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory index = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter writer = new IndexWriter(index, config);
    int counter = 0;
    long start = System.currentTimeMillis();
    /*
    reviewerID
    asin
    reviewerName
    helpful
    reviewText
     */
    try (Scanner scanner = new Scanner(new FileReader("reviews/reviews.json"))) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
        counter++;
        if (counter % 10000 == 0) {
          System.out.println("counter = " + counter);
        }
        Document document = new Document();
        String id = jsonObject.get("asin").getAsString();
        document.add(new StringField("id", id, Field.Store.YES));

        String review = jsonObject.get("reviewText").getAsString();
        document.add(new TextField("review", review, Field.Store.YES));
        writer.addDocument(document);
      }
    }
    writer.close();
    long end = System.currentTimeMillis();
    System.out.println("(end - start) = " + (end - start));
    System.out.println("numReviews = " + counter);
  }
}
