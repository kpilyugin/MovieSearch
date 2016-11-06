import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static edu.stanford.nlp.ling.CoreAnnotations.*;

public class EntityExtractor {

  private final StanfordCoreNLP pipeline;

  public EntityExtractor() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
    pipeline = new StanfordCoreNLP(props);
  }

  private void extract(String text, PrintWriter writer) {
    Annotation document = new Annotation(text);
    pipeline.annotate(document);
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    List<CoreLabel> tokens = new ArrayList<>();
    for (CoreMap sentence : sentences) {
      tokens.addAll(sentence.get(TokensAnnotation.class));
    }
    writer.println("tokens.size() = " + tokens.size());

    Map<String, List<CoreLabel>> namedEntities = tokens.stream()
        .collect(Collectors.groupingBy(token -> token.get(NamedEntityTagAnnotation.class)));
    writer.println("all entities: " + namedEntities.keySet());
    writer.println("locations: " + words(namedEntities.get("LOCATION")));
    writer.println("dates: " + words(namedEntities.get("DATE")));
    writer.println("persons: " + words(namedEntities.get("PERSON")));
  }

  private List<String> words(List<CoreLabel> labels) {
    return labels == null ? Collections.emptyList() :
        labels.stream()
            .map(label -> label.get(TextAnnotation.class))
            .collect(Collectors.toList());
  }

  public static void start() {
    EntityExtractor extractor = new EntityExtractor();
    try (Scanner scanner = new Scanner(System.in);
         PrintWriter writer = new PrintWriter(System.out)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        extractor.extract(line, writer);
        writer.flush();
      }
    }
  }
}
