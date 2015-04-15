/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.util.hibernate;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.constants.WebsiteConstants;
import edu.colorado.phet.website.content.simulations.AbstractSimulationPage;
import edu.colorado.phet.website.data.Category;
import edu.colorado.phet.website.data.LocalizedSimulation;
import edu.colorado.phet.website.data.Project;
import edu.colorado.phet.website.data.Simulation;
import edu.colorado.phet.website.data.Translation;
import edu.colorado.phet.website.translation.PhetLocalizer;
import edu.colorado.phet.website.util.StringUtils;

/**
 * Collections of utility functions that mainly deal with the website's interaction with Hibernate
 * <p/>
 * TODO: move other functions that aren't primarily related to hibernate elsewhere
 */
public class HibernateUtils {

    private static org.hibernate.SessionFactory sessionFactory;

    private static final Logger logger = Logger.getLogger( HibernateUtils.class.getName() );

    private HibernateUtils() {
        // don't instantiate
        throw new AssertionError();
    }

    static {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    public static SessionFactory getInstance() {
        // TODO: examine openSessions and
        return sessionFactory;
    }

    public Session openSession() {
        return sessionFactory.openSession();
    }

    public Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public static void close() {
        if ( sessionFactory != null ) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }

    public static Project getOtherProject( Session session, String simulationName, Project project ) {
        List<Simulation> sims = session.createQuery( "select s from Simulation as s where s.name = :name" ).setString( "name", simulationName ).list();
        for ( Simulation s : sims ) {
            if ( s.isVisible() && s.getProject().getId() != project.getId() ) {
                return s.getProject();
            }
        }

        // try looking up the sim from the mapping in case it is not found due to a name mismatch between the legacy and html versions
        if ( AbstractSimulationPage.LEGACY_TO_CURRENT_SIM_NAME.containsKey( simulationName ) ) {
            simulationName = AbstractSimulationPage.LEGACY_TO_CURRENT_SIM_NAME.get( simulationName );
        }
        else if ( AbstractSimulationPage.CURRENT_SIM_NAME_TO_LEGACY.containsKey( simulationName ) ) {
            simulationName = AbstractSimulationPage.CURRENT_SIM_NAME_TO_LEGACY.get( simulationName );
        }
        else {
            return null;
        }

        List<Simulation> alternateSims = session.createQuery( "select s from Simulation as s where s.name = :name" ).setString( "name", simulationName ).list();
        for ( Simulation s : alternateSims ) {
            if ( s.isVisible() && s.getProject().getId() != project.getId() ) {
                return s.getProject();
            }
        }
        return null;
    }

    public static List<LocalizedSimulation> getLocalizedSimulationsMatching( Session session, String projectName, String simulationName, Locale locale ) {

        boolean useSimulation = simulationName != null || projectName != null;
        boolean useProject = projectName != null;

        // select
        String queryString = "select l from LocalizedSimulation as l";
        if ( useSimulation ) {
            queryString += ", Simulation as s";
        }
        if ( useProject ) {
            queryString += ", Project as p";
        }

        // where conditions
        queryString += " where (";

        List<String> conditions = new LinkedList<String>();

        // joins
        if ( useSimulation ) {
            conditions.add( "l.simulation = s" );
        }
        if ( useProject ) {
            conditions.add( "s.project = p" );
        }

        // constraints
        if ( projectName != null ) {
            conditions.add( "p.name = :project" );
        }
        if ( simulationName != null ) {
            conditions.add( "s.name = :flavor" );
        }
        if ( locale != null ) {
            conditions.add( "l.locale = :locale" );
        }

        boolean prev = false;
        for ( String condition : conditions ) {
            if ( prev ) {
                queryString += " AND ";
            }
            queryString += condition;
            prev = true;
        }

        queryString += ")";

        Query query = session.createQuery( queryString );

        if ( projectName != null ) {
            query.setString( "project", projectName );
        }
        if ( simulationName != null ) {
            query.setString( "flavor", simulationName );
        }
        if ( locale != null ) {
            query.setLocale( "locale", locale );
        }

        List simulations = query.list();
        List<LocalizedSimulation> ret = new LinkedList<LocalizedSimulation>();
        for ( Object simulation : simulations ) {
            ret.add( (LocalizedSimulation) simulation );
        }
        return ret;

    }

    public static Category getCategoryByName( Session session, String categoryName ) {
        return (Category) session.createQuery( "select c from Category as c where c.name = :name" ).setString( "name", categoryName ).uniqueResult();
    }

    public static List<LocalizedSimulation> getCategorySimulationsWithLocale( Session session, Category category, Locale locale ) {
        Query query = session.createQuery( "select l from LocalizedSimulation as l, Simulation as s, Category as c where (l.simulation = s AND (s in elements(c.simulations)) AND l.locale = :locale AND c = :category) order by indices(c)" );
        query.setLocale( "locale", locale );
        query.setEntity( "category", category );
        List simulations = query.list();
        List<LocalizedSimulation> ret = new LinkedList<LocalizedSimulation>();
        for ( Object simulation : simulations ) {
            ret.add( (LocalizedSimulation) simulation );
        }
        return ret;
    }

    public static List<LocalizedSimulation> getAllVisibleSimulationsWithLocale( Session session, Locale locale ) {
        List simulations = session.createQuery( "select l from LocalizedSimulation as l where l.locale = :locale and l.simulation.project.visible = true and l.simulation.simulationVisible = true" ).setLocale( "locale", locale ).list();
        List<LocalizedSimulation> ret = new LinkedList<LocalizedSimulation>();
        for ( Object simulation : simulations ) {
            ret.add( (LocalizedSimulation) simulation );
        }
        return ret;
    }

    /**
     * Find the best LocalizedSimulation that matches the given locale and simulation name.
     *
     * <p/>
     * NOTE: Simulation names should be unique. Matches exact locale, then language, then English.
     * NOTE: Session must be within a transaction
     *
     * @param session    Hibernate session (in a transaction)
     * @param locale     Desired locale
     * @param simulation Simulation name
     * @param isLegacy   Looks for legacy sim matches if true, otherwise will return an html sim page match if one exists
     * @return The best LocalizedSimulation
     */
    public static LocalizedSimulation getBestSimulation( Session session, Locale locale, String simulation, boolean isLegacy ) {
        Locale englishLocale = LocaleUtils.stringToLocale( "en" );
        Locale languageLocale = LocaleUtils.stringToLocale( locale.getLanguage() );
        boolean useLanguage = !languageLocale.equals( locale );

        Query query = session.createQuery( "select l from LocalizedSimulation as l, Simulation as s, Project as p where (l.simulation = s AND s.project = p AND s.name = :flavor AND (l.locale = :english OR l.locale = :locale" + ( useLanguage ? " OR l.locale = :lang" : "" ) + "))" );
        query.setString( "flavor", simulation );
        query.setLocale( "locale", locale );
        if ( useLanguage ) {
            query.setLocale( "lang", languageLocale );
        }
        query.setLocale( "english", englishLocale );
        List<LocalizedSimulation> simulations = query.list();

        if ( simulations.size() == 0 ) {
            return null;
        }

        if ( simulations.size() == 1 ) {
            return simulations.get( 0 );
        }

        if ( !isLegacy ) {
            // Get HTML sims if there are some
            List<LocalizedSimulation> htmlSimulations = new LinkedList();
            for ( LocalizedSimulation localizedSim : simulations ) {
                if ( localizedSim.getSimulation().getProject().getType() == Project.TYPE_HTML && localizedSim.getSimulation().isVisible() ) {
                    htmlSimulations.add( localizedSim );
                }
            }

            // HTML sims are returned first
            if ( htmlSimulations.size() > 0 ) {
                return htmlSimulations.get( 0 );
            }
        }

        if ( simulations.size() <= 3 ) {
            for ( LocalizedSimulation localizedSim : simulations ) {
                if ( localizedSim.getLocale().equals( locale ) ) {
                    return localizedSim;
                }
            }
            if ( useLanguage ) {
                for ( LocalizedSimulation localizedSim : simulations ) {
                    if ( localizedSim.getLocale().equals( languageLocale ) ) {
                        return localizedSim;
                    }
                }
            }
            return simulations.get( 0 );
        }

        throw new RuntimeException( "WARNING: matches more than 3 simulations!" );
    }

    public static LocalizedSimulation getBestSimulation( Session session, Locale locale, String simulation ) {
        return getBestSimulation( session, locale, simulation, true );
    }

    public static final String[] SIM_TITLE_IGNORE_WORDS = { "The", "La", "El" };

    public static String getLeadingSimCharacter( String name, Locale locale ) {
        String str = name;
        for ( String ignoreWord : SIM_TITLE_IGNORE_WORDS ) {
            if ( str.startsWith( ignoreWord + " " ) ) {
                str = str.substring( ignoreWord.length() + 1 );
            }
        }
        return str.trim().substring( 0, 1 ).toUpperCase( locale );
    }

    public static String encodeCharacterId( String chr ) {
        /*
        StringBuffer buf = new StringBuffer();
        byte[] bytes = chr.getBytes();
        for ( Byte b : bytes ) {
            buf.append( Integer.toHexString( b.intValue() ) );
        }
        return buf.toString();
        */
        if ( chr.trim().length() == 0 ) {
            return "-";
        }
        return chr;
    }

    /**
     * Sort a list of localized simulations for a particular locale. This means simulations will be sorted
     * first by the title in the locale parameter (if there is a title), then by locale.
     *
     * @param list   The list of simulations to order
     * @param locale The locale to use for ordering
     */
    public static void orderSimulations( List<LocalizedSimulation> list, final Locale locale ) {
        final HashMap<String, String> map = new HashMap<String, String>();
        final PhetLocalizer phetLocalizer = (PhetLocalizer) PhetWicketApplication.get().getResourceSettings().getLocalizer();
        final Collator collator = Collator.getInstance( locale );

        for ( LocalizedSimulation sim : list ) {
            boolean correctLocale = locale.equals( sim.getLocale() );
            if ( !map.containsKey( sim.getSimulation().getName() ) || correctLocale ) {
                if ( correctLocale ) {
                    map.put( sim.getSimulation().getName(), sim.getTitle() );
                }
                else {
                    map.put( sim.getSimulation().getName(), null );
                }
            }
        }

        Collections.sort( list, new Comparator<LocalizedSimulation>() {
            public int compare( LocalizedSimulation a, LocalizedSimulation b ) {

                boolean aTranslated = a.getLocale().equals( locale );
                boolean bTranslated = b.getLocale().equals( locale );

                if ( !bTranslated && aTranslated ) {
                    return -1;
                }

                if ( !aTranslated && bTranslated ) {
                    return 1;
                }

                if ( a.getSimulation().getName().equals( b.getSimulation().getName() ) ) {
                    if ( a.getLocale().equals( locale ) ) {
                        return -1;
                    }
                    if ( b.getLocale().equals( locale ) ) {
                        return 1;
                    }

                    String localeA = StringUtils.getLocaleTitle( a.getLocale(), locale, phetLocalizer );
                    String localeB = StringUtils.getLocaleTitle( b.getLocale(), locale, phetLocalizer );
                    return collator.compare( localeA, localeB );
                }

                String aGlobalTitle = map.get( a.getSimulation().getName() );
                String bGlobalTitle = map.get( b.getSimulation().getName() );

                boolean aGlobal = aGlobalTitle != null;
                boolean bGlobal = bGlobalTitle != null;

                if ( aGlobal && bGlobal ) {

                    for ( String ignoreWord : SIM_TITLE_IGNORE_WORDS ) {
                        if ( aGlobalTitle.startsWith( ignoreWord + " " ) ) {
                            aGlobalTitle = aGlobalTitle.substring( ignoreWord.length() + 1 );
                        }
                        if ( bGlobalTitle.startsWith( ignoreWord + " " ) ) {
                            bGlobalTitle = bGlobalTitle.substring( ignoreWord.length() + 1 );
                        }
                    }
                    return collator.compare( aGlobalTitle, bGlobalTitle );
                }
                else if ( aGlobal ) {
                    return -1;
                }
                else if ( bGlobal ) {
                    return 1;
                }
                else {
                    return collator.compare( a.getSimulation().getName(), b.getSimulation().getName() );
                }
            }
        } );
    }

    /**
     * Used for translation panels / etc, so this should preferably return a simulation in the preferredLocale if it exists
     *
     * @param session         Hibernate session
     * @param preferredLocale Desired locale of the simulation
     * @return A LocalizedSimulation instance
     */
    public static LocalizedSimulation getExampleSimulation( Session session, Locale preferredLocale ) {
        LocalizedSimulation simulation = null;
        Query query = session.createQuery( "select ls from LocalizedSimulation as ls, Simulation as s where (ls.simulation = s and s.name = 'circuit-construction-kit-dc' and ls.locale = :locale)" );
        query.setLocale( "locale", preferredLocale );
        List list = query.list();
        if ( !list.isEmpty() ) {
            simulation = (LocalizedSimulation) list.get( 0 );
        }
        else {
            query = session.createQuery( "select ls from LocalizedSimulation as ls, Simulation as s where (ls.simulation = s and ls.locale = :locale)" );
            query.setLocale( "locale", preferredLocale );
            list = query.list();
            if ( !list.isEmpty() ) {
                simulation = (LocalizedSimulation) list.get( 0 );
            }
            else {
                simulation = (LocalizedSimulation) session.createQuery( "select ls from LocalizedSimulation as ls, Simulation as s where (ls.simulation = s and s.name = 'circuit-construction-kit-dc' and ls.locale = :locale)" ).setLocale( "locale", LocaleUtils.stringToLocale( "en" ) ).uniqueResult();
            }
        }
        return simulation;
    }

    public static List<Translation> getVisibleTranslations( Session session ) {
        List<Translation> ret = new LinkedList<Translation>();
        List li = session.createQuery( "select t from Translation as t where t.visible = true" ).list();

        for ( Object o : li ) {
            Translation translation = (Translation) o;
            if ( translation.getLocale().equals( WebsiteConstants.ENGLISH ) ) {
                continue;
            }
            ret.add( translation );
        }

        return ret;
    }

    public static LocalizedSimulation pickBestTranslation( Simulation sim, Locale locale ) {
        LocalizedSimulation defaultSim = null;
        LocalizedSimulation languageDefaultSim = null;
        for ( Object o : sim.getLocalizedSimulations() ) {
            LocalizedSimulation lsim = (LocalizedSimulation) o;
            if ( lsim.getLocale().equals( locale ) ) {
                return lsim;
            }
            else if ( lsim.getLocale().equals( WebsiteConstants.ENGLISH ) ) {
                defaultSim = lsim;
            }
            else if ( lsim.getLocale().getLanguage().equals( locale.getLanguage() ) ) {
                languageDefaultSim = lsim;
            }
        }
        return languageDefaultSim == null ? defaultSim : languageDefaultSim;
    }

    public static void addPreferredFullSimulationList( List<LocalizedSimulation> lsims, Session session, Locale locale, boolean listBothVersions ) {
        logger.debug( "1" );
        Criteria criteria = session.createCriteria( Simulation.class )
                .setFetchMode( "localizedSimulations", FetchMode.SELECT )
                .add( Restrictions.eq( "simulationVisible", true ) );
        criteria.createCriteria( "project" ).add( Restrictions.eq( "visible", true ) );
        //List sims = session.createQuery( "select s from Simulation as s where s.project.visible = true and s.simulationVisible = true" ).list();
        List sims = criteria.list();
        Set simsWithTwoVersions = getSimsWithTwoVersions( sims );
        logger.debug( "2" );
        for ( Object sim : sims ) {
            Simulation simulation = (Simulation) sim;
            if ( !simulation.isVisible() ) {
                continue;
            }
            if ( !listBothVersions && simsWithTwoVersions.contains( simulation.getName() ) && simulation.getProject().getType() != Project.TYPE_HTML ) {
                continue;
            }
            lsims.add( pickBestTranslation( simulation, locale ) );
        }
        logger.debug( "3" );
    }

    public static void addPreferredFullSimulationList( List<LocalizedSimulation> lsims, Session session, Locale locale ) {
        addPreferredFullSimulationList( lsims, session, locale, false );
    }

        /**
         * This method is used when displaying the sims by category to filter out legacy versions
         * @param simulations A list of Simulations to search through
         * @return A set of sim names that have multiple version (html and legacy)
         */
    public static Set<String> getSimsWithTwoVersions( List simulations ) {
        // sims that appear twice must have an html5 version
        Set<String> simsSet = new HashSet<String>();
        Set<String> simsWithTwoVersions = new HashSet<String>();
        for ( Object o : simulations ) {
            Simulation sim = (Simulation) o;
            if ( simsSet.contains( sim.getName() ) || AbstractSimulationPage.LEGACY_TO_CURRENT_SIM_NAME.containsKey( sim.getName() ) ) {
                simsWithTwoVersions.add( sim.getName() );
            }
            simsSet.add( sim.getName() );
        }
        return simsWithTwoVersions;
    }

    public static List<LocalizedSimulation> preferredFullSimulationList( Session session, Locale locale ) {
        LinkedList<LocalizedSimulation> ret = new LinkedList<LocalizedSimulation>();
        addPreferredFullSimulationList( ret, session, locale, true );
        return ret;
    }

    /**
     * Wraps an action within a session opening and transaction scope. Handles runtime exceptions.
     * Use wrapTransaction directly if you have access to a requestcycle. This is mainly meant for use when initializing
     * the application.
     *
     * @param task The task to run
     * @return Success (false if task.run returns false OR a runtime exception occurs).
     */
    public static boolean wrapSession( HibernateTask task ) {
        Session session = getInstance().openSession();
        boolean ret = false;
        try {
            ret = wrapTransaction( session, task );
        }
        finally {
            session.close();
        }
        return ret;
    }

    /**
     * Wraps an action within a transaction scope. Handles runtime exceptions.
     *
     * @param session Session to use
     * @param task    Task to run
     * @return Success (false if task.run returns false OR a runtime exception occurs).
     */
    public static boolean wrapTransaction( Session session, HibernateTask task ) {
        Transaction tx = null;
        boolean ret;
        try {
            tx = session.beginTransaction();
            tx.setTimeout( 600 );

            ret = task.run( session );

            //logger.debug( "tx isactive: " + tx.isActive() );
            //logger.debug( "tx wascommited: " + tx.wasCommitted() );
            if ( tx.isActive() ) {
                tx.commit();
            }
            else {
                //logger.warn( "tx not active", new RuntimeException( "exception made for stack trace" ) );
            }
        }
        catch ( RuntimeException e ) {
            ret = false;
            logger.warn( "Exception", e );
            if ( tx != null && tx.isActive() ) {
                try {
                    logger.warn( "Attempting to roll back" );
                    tx.rollback();
                }
                catch ( HibernateException e1 ) {
                    logger.error( "ERROR: Error rolling back transaction!", e1 );
                }
                throw e;
            }
        }
        return ret;
    }

    /*---------------------------------------------------------------------------*
    * hibernate testing below
    *----------------------------------------------------------------------------*/

    public static void tryRollback( Transaction tx ) {
        if ( tx != null && tx.isActive() ) {
            try {
                logger.info( "Attempting to roll back" );
                tx.rollback();
            }
            catch ( HibernateException e1 ) {
                logger.error( "ERROR: Error rolling back transaction!", e1 );
                throw e1;
            }
        }
    }

    // TODO: organization!!!

    public static <T> Result<T> resultTransaction( Session session, Task<T> task ) {
        return transactionCore( session, task, true );
    }

    public static boolean resultTransaction( Session session, final VoidTask task ) {
        return resultTransaction( session, voidToVoid( task ) ).success;
    }

    // TODO: make a version of this that will not print out errors?
    public static <T> Result<T> resultCatchTransaction( Session session, Task<T> task ) {
        return transactionCore( session, task, false );
    }

    public static boolean resultCatchTransaction( Session session, final VoidTask task ) {
        return resultCatchTransaction( session, voidToVoid( task ) ).success;
    }

    public static <T> T ensureTransaction( Session session, Task<T> task ) {
        if ( session.getTransaction().isActive() ) {
            // if we have a failure, it will be handled at the preceeding catch blocks
            return task.run( session );
        }
        else {
            // we need to put it inside of a transaction, so we will wrap it here
            Result<T> result = resultTransaction( session, task );
            if ( result.success ) {
                return result.value;
            }
            else {
                // make sure we exception-out if we had a failure (particularly a task exception!!!)
                throw new RuntimeException( result.exception );
            }
        }
    }

    public static boolean ensureTransaction( Session session, VoidTask task ) {
        return ensureTransaction( session, voidToBoolean( task ) );
    }

    public static boolean wrapTransaction( Session session, Task<Void> task ) {
        return transactionCore( session, task, true ).success;
    }

    public static boolean wrapTransaction( Session session, final VoidTask task ) {
        return transactionCore( session, voidToVoid( task ), true ).success;
    }

    public static boolean wrapCatchTransaction( Session session, Task<Void> task ) {
        return transactionCore( session, task, false ).success;
    }

    public static boolean wrapCatchTransaction( Session session, final VoidTask task ) {
        return transactionCore( session, voidToVoid( task ), false ).success;
    }

    private static <T> Result<T> transactionCore( Session session, Task<T> task, boolean throwHibernateExceptions ) {
        Transaction tx = null;
        T ret = null;
        try {
            tx = session.beginTransaction();
            tx.setTimeout( 600 );

            ret = task.run( session );

            if ( tx.isActive() ) {
                tx.commit();
            }

            return new Result<T>( true, ret, null );
        }
        catch ( TaskException e ) {
            // TODO: check current levels of TaskExceptions
            logger.log( e.level, "exception: ", e ); // log the TaskException at the desired level
            tryRollback( tx );
            return new Result<T>( false, ret, e );
        }
        catch ( RuntimeException e ) {
            logger.warn( "exception: ", e );
            tryRollback( tx );
            if ( throwHibernateExceptions ) {
                throw e;
            }
            else {
                return new Result<T>( false, ret, e );
            }
        }
    }

    public static <T> Result<T> load( Session session, final Class clazz, final int id ) {
        // TODO: more generic option from this?
        return resultCatchTransaction( session, new Task<T>() {
            public T run( Session session ) {
                return (T) session.load( clazz, id );
            }
        } );
    }

    public static <T> Result<T> resultSession( Task<T> task ) {
        return sessionCore( task, true );
    }

    public static <T> Result<T> resultCatchSession( Task<T> task ) {
        return sessionCore( task, false );
    }

    public static boolean wrapSession( Task<Void> task ) {
        return sessionCore( task, true ).success;
    }

    public static boolean wrapSession( VoidTask task ) {
        return sessionCore( voidToVoid( task ), true ).success;
    }

    public static boolean wrapCatchSession( Task<Void> task ) {
        return sessionCore( task, false ).success;
    }

    private static <T> Result<T> sessionCore( Task<T> task, boolean throwHibernateExceptions ) {
        Session session = getInstance().openSession();
        Result<T> ret = transactionCore( session, task, throwHibernateExceptions );
        session.close();
        return ret;
    }

    /**
     * Get the publicly-visible translation by its Locale
     *
     * @param session Hibernate session - transaction optional
     * @param locale  Locale
     * @return Translation
     */
    public static Translation getTranslation( Session session, final Locale locale ) {
        return ensureTransaction( session, new Task<Translation>() {
            public Translation run( Session session ) {
                return (Translation) session.createQuery( "select t from Translation as t where t.visible = true and t.locale = :locale" ).setLocale( "locale", locale ).uniqueResult();
            }
        } );
    }

    /**
     * Translation lookup by ID
     *
     * @param session       Hibernate session - transaction optional
     * @param translationId Translation ID
     * @return Translation
     */
    public static Translation getTranslation( Session session, final int translationId ) {
        return ensureTransaction( session, new Task<Translation>() {
            public Translation run( Session session ) {
                return (Translation) session.load( Translation.class, translationId );
            }
        } );
    }

    private static Task<Void> voidToVoid( final VoidTask task ) {
        return new Task<Void>() {
            public Void run( Session session ) {
                task.run( session );
                return null;
            }
        };
    }

    private static Task<Boolean> voidToBoolean( final VoidTask task ) {
        return new Task<Boolean>() {
            public Boolean run( Session session ) {
                task.run( session );
                return true; // indicates success
            }
        };
    }

}