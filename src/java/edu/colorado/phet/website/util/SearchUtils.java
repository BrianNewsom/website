package edu.colorado.phet.website.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.hibernate.Session;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.data.Category;
import edu.colorado.phet.website.data.Keyword;
import edu.colorado.phet.website.data.LocalizedSimulation;
import edu.colorado.phet.website.data.Simulation;
import edu.colorado.phet.website.menu.NavMenu;
import edu.colorado.phet.website.translation.PhetLocalizer;

public class SearchUtils {

    // TODO: extract into better writing / reading / updating, and add contributions

    // TODO: catch index exceptions to try triggering re-building or fail-out?

    private static Logger logger = Logger.getLogger( SearchUtils.class.getName() );

    private static Directory directory = null;

    private static IndexSearcher searcher = null;
    private static IndexWriter writer = null;
    private static Thread indexerThread = null;

    /**
     * Don't call this more than once
     */
    public static void initialize() {
        if ( directory != null ) {
            throw new RuntimeException( "attempt to initialize SearchUtils multiple times" );
        }
        try {

            final PhetWicketApplication app = PhetWicketApplication.get();
            final PhetLocalizer localizer = PhetLocalizer.get();

            directory = FSDirectory.open( new File( "/tmp/testindex" ) );

            reindex( app, localizer );
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    public static synchronized void destroy() {
        try {
            if ( searcher != null ) {
                searcher.close();
            }
            if ( directory != null ) {
                directory.close();
            }
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    public static synchronized IndexSearcher getSearcher() {
        return searcher;
    }

    public static synchronized void reindex( final PhetWicketApplication app, final PhetLocalizer localizer ) {
        indexerThread = new Thread() {
            @Override
            public void run() {
                logger.info( "starting indexing thread" );
                addAllDocuments( app, localizer );
                logger.info( "indexing complete" );

                try {
                    searcher = new IndexSearcher( directory, true );
                }
                catch( IOException e ) {
                    e.printStackTrace();
                }
            }
        };
        indexerThread.setPriority( Thread.MIN_PRIORITY );
        indexerThread.start();
    }

    private static synchronized void addAllDocuments( final PhetWicketApplication app, final PhetLocalizer localizer ) {
        try {
            Analyzer analyzer = new StandardAnalyzer( Version.LUCENE_CURRENT );
            writer = new IndexWriter( directory, analyzer, true, new IndexWriter.MaxFieldLength( 25000 ) );

            HibernateUtils.wrapSession( new HibernateTask() {
                public boolean run( Session session ) {
                    addSimulations( session, localizer, app.getMenu() );
                    return true;
                }
            } );

            writer.optimize();
            writer.close();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    private static synchronized void removeAllDocuments() {
        try {
            final IndexReader reader = IndexReader.open( directory, false );
            int dropped = reader.deleteDocuments( new Term( "droppable", "true" ) );
            logger.info( "dropped " + dropped + " documents" );
            reader.close();
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    private static void addSimulations( Session session, final PhetLocalizer localizer, final NavMenu menu ) {
        try {
            logger.debug( "adding simulations" );

            List s = session.createQuery( "select s from Simulation as s" ).list();
            for ( Object o : s ) {
                Simulation sim = (Simulation) o;
                logger.debug( "processing " + sim.getName() );
                Document doc = simulationToDocument( session, sim, localizer, menu );
                logger.debug( "processed" );
                writer.addDocument( doc );
                logger.debug( "added" );
                //logger.debug( "adding document: " + doc );
            }
        }
        catch( IOException e ) {
            e.printStackTrace();
        }
    }

    public static Document simulationToDocument( Session session, Simulation sim, PhetLocalizer localizer, NavMenu menu ) {
        Document doc = new Document();
        doc.add( new Field( "droppable", "true", Field.Store.NO, Field.Index.NOT_ANALYZED ) );
        doc.add( new Field( "sim_id", String.valueOf( sim.getId() ), Field.Store.YES, Field.Index.NOT_ANALYZED ) );
        Field nameField = new Field( "sim_name", sim.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED );
        nameField.setBoost( 0.3f );
        doc.add( nameField );
        for ( Object o1 : sim.getLocalizedSimulations() ) {
            LocalizedSimulation lsim = (LocalizedSimulation) o1;
            String prefix = lsim.getLocaleString();
            Field titleField = new Field( "sim_" + prefix + "_title", lsim.getTitle(), Field.Store.YES, Field.Index.ANALYZED );
            titleField.setBoost( 4.5f );
            doc.add( titleField );

            String description = localizer.getBestStringWithinTransaction( session, sim.getDescriptionKey(), lsim.getLocale() );
            if ( description != null ) {
                doc.add( new Field( "sim_" + prefix + "_description", description, Field.Store.NO, Field.Index.ANALYZED ) );
            }

            String goals = localizer.getBestStringWithinTransaction( session, sim.getLearningGoalsKey(), lsim.getLocale() );
            if ( goals != null ) {
                doc.add( new Field( "sim_" + prefix + "_goals", goals, Field.Store.NO, Field.Index.ANALYZED ) );
            }

            String keywords = "";
            for ( Object o2 : sim.getKeywords() ) {
                Keyword keyword = (Keyword) o2;
                String key = localizer.getBestStringWithinTransaction( session, keyword.getLocalizationKey(), lsim.getLocale() );
                if ( key != null ) {
                    keywords += key + " ";
                }
            }
            if ( keywords.length() > 0 ) {
                doc.add( new Field( "sim_" + prefix + "_keywords", keywords, Field.Store.NO, Field.Index.ANALYZED ) );
            }

            String topics = "";
            for ( Object o2 : sim.getTopics() ) {
                Keyword keyword = (Keyword) o2;
                String key = localizer.getBestStringWithinTransaction( session, keyword.getLocalizationKey(), lsim.getLocale() );
                if ( key != null ) {
                    topics += key + " ";
                }
            }
            if ( topics.length() > 0 ) {
                doc.add( new Field( "sim_" + prefix + "_topics", topics, Field.Store.NO, Field.Index.ANALYZED ) );
            }

            String categories = "";
            for ( Object o2 : sim.getCategories() ) {
                Category category = (Category) o2;
                String key = localizer.getBestStringWithinTransaction( session, category.getNavLocation( menu ).getLocalizationKey(), lsim.getLocale() );
                if ( key != null ) {
                    categories += key + " ";
                }
            }
            if ( categories.length() > 0 ) {
                doc.add( new Field( "sim_" + prefix + "_categories", categories, Field.Store.NO, Field.Index.ANALYZED ) );
            }
        }
        return doc;
    }

    private static void addBoostedTermQuery( BooleanQuery query, String field, String term, float boost ) {
        TermQuery tquery = new TermQuery( new Term( field, term ) );
        tquery.setBoost( boost );
        query.add( tquery, BooleanClause.Occur.SHOULD );
    }

    public static List<LocalizedSimulation> testSearch( Session session, String queryString, final Locale locale ) {
        final List<LocalizedSimulation> ret = new LinkedList<LocalizedSimulation>();

        final IndexSearcher isearcher = getSearcher();

        try {
            StringTokenizer tokenizer = new StringTokenizer( queryString );
            BooleanQuery query = new BooleanQuery();

            String localeString = LocaleUtils.localeToString( locale );

            while ( tokenizer.hasMoreTokens() ) {
                String term = tokenizer.nextToken();
                addBoostedTermQuery( query, "sim_name", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_title", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_description", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_goals", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_keywords", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_topics", term, 1.0f );
                addBoostedTermQuery( query, "sim_en_categories", term, 1.0f );
                if ( !localeString.equals( "en" ) ) {
                    addBoostedTermQuery( query, "sim_" + localeString + "_title", term, 4.0f );
                    addBoostedTermQuery( query, "sim_" + localeString + "_description", term, 4.0f );
                    addBoostedTermQuery( query, "sim_" + localeString + "_goals", term, 4.0f );
                    addBoostedTermQuery( query, "sim_" + localeString + "_keywords", term, 4.0f );
                    addBoostedTermQuery( query, "sim_" + localeString + "_topics", term, 4.0f );
                    addBoostedTermQuery( query, "sim_" + localeString + "_categories", term, 4.0f );
                }
            }

            logger.debug( "query: " + query );

            final ScoreDoc[] hits = isearcher.search( query, null, 1000 ).scoreDocs;

            HibernateUtils.wrapTransaction( session, new HibernateTask() {
                public boolean run( Session session ) {
                    for ( ScoreDoc hit : hits ) {
                        try {
                            float score = hit.score;
                            Document doc = isearcher.doc( hit.doc );
                            Simulation sim = (Simulation) session.load( Simulation.class, Integer.parseInt( doc.get( "sim_id" ) ) );
                            logger.debug( score + ": " + sim.getName() + " " + doc.get( "sim_en_title" ) );
                            LocalizedSimulation lsim = sim.getBestLocalizedSimulation( locale );
                            if ( lsim != null ) {
                                ret.add( lsim );
                            }
                        }
                        catch( IOException e ) {
                            e.printStackTrace();
                        }

                    }
                    return true;
                }
            } );
        }
        catch( IOException e ) {
            e.printStackTrace();
        }

        return ret;
    }

    public static void main( String[] args ) throws IOException, ParseException {

//        Analyzer analyzer = new StandardAnalyzer( Version.LUCENE_CURRENT );
//
//        Directory directory = new RAMDirectory();
//        //Directory directory = FSDirectory.open("/tmp/testindex");
//
//        IndexWriter iwriter = new IndexWriter( directory, analyzer, true, new IndexWriter.MaxFieldLength( 25000 ) );
//        Document doc = new Document();
//        doc.add( new Field( "fieldname", "This is the text to be indexed.", Field.Store.YES, Field.Index.ANALYZED ) );
//        iwriter.addDocument( doc );
//        iwriter.close();
//
//        IndexSearcher isearcher = new IndexSearcher( directory, true );
//        QueryParser parser = new QueryParser( Version.LUCENE_CURRENT, "fieldname", analyzer );
//        Query query = parser.parse( "text" );
//        ScoreDoc[] hits = isearcher.search( query, null, 1000 ).scoreDocs;
//
//        for ( int i = 0; i < hits.length; i++ ) {
//            Document hitDoc = isearcher.doc( hits[i].doc );
//            System.out.println( "score: " + hits[i].score );
//            System.out.println( "field: " + hitDoc.get( "fieldname" ) );
//        }
//
//        isearcher.close();
//        directory.close();
    }
}