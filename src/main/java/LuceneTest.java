import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

public class LuceneTest {
    static String indexPath = "index";

    public static void main(String[] args) {
        indexing();
        try {
            search("asdf");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static void search(String queryString) throws IOException, ParseException {
        String field = "content";

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        for (int i=0; i<reader.maxDoc(); i++) {
            Document doc = reader.document(i);
            String docId = doc.get("drugSentenceEng");
            System.out.println("doc " + docId);
        }

        //QueryParser parser = new AnalyzingQueryParser(field, analyzer);

        QueryParser parser = new QueryParser(field, analyzer);


        //String line = "*" + ":" + "sentence";
        //             drugSentenceEng
        String line = "*drugsentenceeng*";

        //Query query = parser.parse(line);
        Query query = new WildcardQuery(new Term(field, line));

        System.out.println("Searching for: " + query.toString(field));


        Date start = new Date();
        TopDocs results = searcher.search(query, 100);
        int numTotalHits = results.totalHits;
        System.out.println(numTotalHits + " total matching documents");
        ScoreDoc[] hits = results.scoreDocs;
        hits = searcher.search(query, numTotalHits).scoreDocs;

        for (int i = 0; i < numTotalHits; i++) {
            System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
            Document doc = searcher.doc(hits[i].doc);
            String path = doc.get("drugSentenceEng");
            System.out.println("Path: " + path);
            continue;
        }

        Date end = new Date();
        System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
        reader.close();
    }

    static void indexing() {
        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);


            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer.  But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            // String drugSentenceEng, String drugSentenceFr, String codeGen
            indexDrug(writer, " drugSentenceEng drugSentenceEng drugSentenceEng  drugSentenceEng", "drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng", "drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng");
            indexDrug(writer, " 1 drugSentenceEng drugSentenceEng drugSentenceEng  drugSentenceEng", "1drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng", "drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng drugSentenceEng1");

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here.  This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    static void indexDrug(IndexWriter writer, String drugSentenceEng, String drugSentenceFr, String codeGen) throws IOException {
        Document doc = new Document();
        Field drugSentenceEngField = new StringField("drugSentenceEng", drugSentenceEng, Field.Store.YES);
        doc.add(drugSentenceEngField);
        Field drugSentenceFrField = new StringField("drugSentenceFr", drugSentenceFr, Field.Store.YES);
        doc.add(drugSentenceFrField);
        Field codeGenField = new StringField("codeGen", codeGen, Field.Store.YES);
        doc.add(codeGenField);
        String fullSearchableText = drugSentenceEng + "" + drugSentenceFr + " " + codeGen;
        doc.add(new TextField("content", fullSearchableText, Field.Store.NO));

        if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):
            System.out.println("adding " + codeGen);
            writer.addDocument(doc);
        } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            System.out.println("updating " + codeGen);
            writer.updateDocument(new Term("codeGenField", codeGen), doc);
        }
    }
}
