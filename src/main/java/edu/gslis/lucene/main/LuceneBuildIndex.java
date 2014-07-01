package edu.gslis.lucene.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import edu.gslis.lucene.indexer.Indexer;
import edu.gslis.lucene.indexer.TrecTextIndexer;
import edu.gslis.lucene.indexer.WikiTextIndexer;
import edu.gslis.lucene.main.config.CorpusConfig;
import edu.gslis.lucene.main.config.FieldConfig;
import edu.gslis.lucene.main.config.IndexConfig;


/**
 * Lucene-backed index builder.  Requires a Yaml-based configuration file.
 * 
 * A few things to note:
 * 
 * 1. Analyzer.  This assumes any configurable analyzer is derived from StopwordsAnalyzerBase.
 * 2. Similarity. Assumes DefaultSimilarity
 * 3. 
 */
public class LuceneBuildIndex {
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    IndexConfig config;
    
    public LuceneBuildIndex(IndexConfig config) {
        this.config = config;
    }
    
    /**
     * Builds a Lucene index given a Yaml configuration file
     * @throws Exception
     */
    public void buildIndex() throws Exception {

        String indexPath = config.getIndexPath();
        Directory dir = FSDirectory.open(new File(indexPath));

        // Initialize the analyzer
        StopwordAnalyzerBase defaultAnalyzer;
        String stopwordsPath = config.getStopwords();
        String analyzerClass = config.getAnalyzer();
        if (!StringUtils.isEmpty(analyzerClass))
        {
            @SuppressWarnings("rawtypes")
            Class analyzerCls = loader.loadClass(analyzerClass);
    
            if (!StringUtils.isEmpty(stopwordsPath))
            {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class, Reader.class);
                defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance(Indexer.VERSION, new FileReader(stopwordsPath));            
            } else {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                java.lang.reflect.Constructor analyzerConst = analyzerCls.getConstructor(Version.class);
                defaultAnalyzer = (StopwordAnalyzerBase)analyzerConst.newInstance(Indexer.VERSION);            
            }
        } else
            defaultAnalyzer = new StandardAnalyzer(Indexer.VERSION);
        
        
        // Assumes default similarity, but can be changed.
        Similarity similarity = new DefaultSimilarity();
        String similarityClass = config.getSimilarity();
        if (!StringUtils.isEmpty(similarityClass))
            similarity = (Similarity)loader.loadClass(similarityClass).newInstance();
        
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<String, Analyzer>();
        Set<FieldConfig> fields = config.getFields();
        for (FieldConfig field: fields) {
            String fieldAnalyzerClass = field.getAnalyzer();
            if (!StringUtils.isEmpty(fieldAnalyzerClass)) {
                @SuppressWarnings("rawtypes")
                Class fieldAnalyzerCls = loader.loadClass(fieldAnalyzerClass);
                Analyzer fieldAnalyzer = (Analyzer)fieldAnalyzerCls.newInstance();
                perFieldAnalyzers.put(field.getName(), fieldAnalyzer);
            }
        }
        
        Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
        IndexWriterConfig iwc = new IndexWriterConfig(Indexer.VERSION, analyzer);
        iwc.setOpenMode(OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(256.0);
        iwc.setSimilarity(similarity);
        
        IndexWriter writer = new IndexWriter(dir, iwc);
        CorpusConfig corpusConfig = config.getCorpus();
        String corpusPath = corpusConfig.getPath();
        String corpusType = corpusConfig.getType();

        try
        {
            if (corpusType.equals(Indexer.FORMAT_WIKI)) {      
                Indexer indexer = new WikiTextIndexer();
                indexer.buildIndex(writer,  fields, new File(corpusPath));
            } else if (corpusType.equals(Indexer.FORMAT_TRECTEXT)){ 
                Indexer indexer = new TrecTextIndexer();
                indexer.buildIndex(writer,  fields, new File(corpusPath));
            } else {
                throw new Exception("Unsupported corpus type/format.");
            }
        } finally {
            writer.close();
        }   
    }
    

    public static void main(String[] args) throws Exception {
        File yamlFile = new File(args[0]);
        if(!yamlFile.exists()) {
            System.err.println("you must specify a configuration file.");
            System.exit(-1);
        }
        
        Yaml yaml = new Yaml(new Constructor(IndexConfig.class));
        IndexConfig config = (IndexConfig)yaml.load(new FileInputStream(yamlFile));

        LuceneBuildIndex builder = new LuceneBuildIndex(config);
        builder.buildIndex();
    }
}
