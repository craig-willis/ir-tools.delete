package edu.gslis.lucene.main;

import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperLuceneImpl;
import edu.gslis.lucene.indexer.Indexer;
import edu.gslis.textrepresentation.FeatureVector;




public class LuceneDumpIndex {
 
    public static void main(String[] args) throws Exception {

        Options options = createOptions();
        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse( options, args);
        
        String path = cl.getOptionValue("index");
        String field = cl.getOptionValue("field");
        String docno = cl.getOptionValue("docno");
        String cmd = cl.getOptionValue("cmd");
        String arg = cl.getOptionValue("arg");

        IndexWrapperLuceneImpl index = new IndexWrapperLuceneImpl(path);
        IndexReader lucene = (IndexReader)index.getActualIndex();
        if (cmd.equals("documentid") || cmd.equals("di")) {
            System.out.println(index.getDocId(field, arg));
        }
        else if (cmd.equals("stats") || cmd.equals("s")) {
            
            Fields fields = MultiFields.getFields(lucene); 
            System.out.println("Documents: \t" + index.docCount());
            System.out.println("Unique terms: \t" + index.termTypeCount());
            System.out.println("Total terms: \t" + index.termCount());
            System.out.println("Fields: \t" + fields.size());
        }
        else if (cmd.equals("documenttext") || cmd.equals("dt")) {
            // Would be nice
        }

        else if (cmd.equals("documentvector") || cmd.equals("dv")) {
            int docId = index.getDocId(docno, arg);
            FeatureVector fv = index.getDocVector(docId, field, null);
            Iterator<String> it = fv.iterator();
            System.out.println("Feature count: " + fv.getFeatureCount());
            System.out.println("Length " + fv.getLength());
            while (it.hasNext()) {
                String f = it.next();
                double w = fv.getFeatureWeight(f);
                System.out.println(f + "\t" + w) ;
            }
        }
        else if (cmd.equals("vocabulary") || cmd.equals("v")) {
            // 1
            Fields fields = MultiFields.getFields(lucene);  
            Terms terms = fields.terms(Indexer.FIELD_TEXT);
            TermsEnum termsEnum = terms.iterator(null);

        }
        else if (cmd.equals("xcount") || cmd.equals("x")) {
            // 3
            // dxcount
        }

        /*
        if (cmd.equals("term") || cmd.equals("t")) {    }
        else if (cmd.equals("termpositions") || cmd.equals("tp")) {}
        else if (cmd.equals("fieldposutions") || cmd.equals("fp")) {}
        else if (cmd.equals("expressionlist") || cmd.equals("e")) {}
        else if (cmd.equals("documentname") || cmd.equals("dn")) {}
        else if (cmd.equals("documentdata") || cmd.equals("dd")) {}
        else if (cmd.equals("invlist") || cmd.equals("il")) {}
         */
    }
    
    public static Options createOptions()
    {
        Options options = new Options();
        options.addOption("index", true, "Path to index");
        options.addOption("field", true, "Field to search");
        options.addOption("docno", true, "Docno field");
        options.addOption("cmd", true, "Command");
        options.addOption("arg", true, "Argument");

        return options;
    }
}
