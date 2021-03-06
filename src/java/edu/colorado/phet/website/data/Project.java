/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.colorado.phet.buildtools.util.ProjectPropertiesFile;
import edu.colorado.phet.common.phetcommon.util.FileUtils;
import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.flashlauncher.util.XMLUtils;
import edu.colorado.phet.website.data.util.IntId;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.HibernateTask;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;
import edu.colorado.phet.website.util.links.RawLinkable;
import edu.colorado.phet.website.util.links.RawLinker;

public class Project implements Serializable, IntId {

    public static final int TYPE_JAVA = 0; // NOTE: update metadata code if you add another type
    public static final int TYPE_FLASH = 1;
    public static final int TYPE_HTML = 2;

    private int id;
    private String name;
    private int type;
    private int versionMajor;
    private int versionMinor;
    private int versionDev;
    private int versionRevision;
    private long versionTimestamp;
    private boolean visible;

    private Set simulations = new HashSet();

    private static final Logger logger = Logger.getLogger( Project.class.getName() );
    private static Logger defaultSyncLogger = Logger.getLogger( Project.class.getName() + ".sync" );

    public Project() {
    }

    public String getVersionString() {
        if ( isHTML() ) {
            return getVersionMajor() + "." + getVersionMinor() + "." + getVersionDev();
        }
        String ret = "";
        ret += getVersionMajor();
        ret += ".";
        if ( getVersionMinor() < 10 && getVersionMinor() >= 0 ) {
            ret += "0";
        }
        ret += getVersionMinor();
        if ( getVersionDev() != 0 ) {
            ret += ".";
            if ( getVersionDev() < 10 && getVersionDev() >= 0 ) {
                ret += "0";
            }
            ret += getVersionDev();
        }
        return ret;
    }

    public ProjectPropertiesFile getProjectPropertiesFile( File docRoot ) {
        return getProjectPropertiesFile( docRoot, name );
    }

    public static ProjectPropertiesFile getProjectPropertiesFile( File docRoot, String projectName ) {
        return new ProjectPropertiesFile( new File( docRoot, "sims/" + projectName + "/" + projectName + ".properties" ) );
    }

    public File getProjectRoot( File docRoot ) {
        return new File( docRoot, "sims/" + name );
    }

    private File getSimulationJARFile( File docRoot, String simulationName, Locale locale ) {
        return new File( getProjectRoot( docRoot ), simulationName + "_" + LocaleUtils.localeToString( locale ) + ".jar" );
    }

    private File getSimulationHTMLFile( File docRoot, String simulationName, Locale locale ) {
        return new File( getLatestHTMLDirectory( getProjectRoot( docRoot ) ), simulationName + "_" + LocaleUtils.localeToString( locale ) + ".html" );
    }

    public File getChangelogFile( File docRoot ) {
        if ( isHTML() ) {
            return new File( getProjectRoot( docRoot ) + "/" + getVersionString(), "changes.txt" );
        }
        return new File( getProjectRoot( docRoot ), "changes.txt" );
    }

    public RawLinkable getRawChangelogLinker() {
        if ( this.isHTML() ) {
            return new RawLinker( "/sims/" + name + "/latest/changes.txt" );
        }
        else {
            return new RawLinker( "/sims/" + name + "/changes.txt" );
        }
    }

    private void appendWarning( StringBuilder builder, String message ) {
        builder.append( "<br/><font color='#FF0000'>WARNING: " ).append( message ).append( "</font>" );
    }

    public List<Simulation> getVisibleSimulations() {
        List<Simulation> ret = new LinkedList<Simulation>();
        for ( Object o : simulations ) {
            Simulation sim = (Simulation) o;
            if ( sim.isVisible() ) {
                ret.add( sim );
            }
        }
        return ret;
    }

    /**
     * Back up the project files to an external directory
     *
     * @param docRoot     Apache document root
     * @param projectName Project name
     * @return Success (true or false)
     */
    public static boolean backupProject( final File docRoot, final String projectName ) {
        return backupProject( docRoot, projectName, false );
    }

    /**
     * Back up the project files to an external directory
     *
     * @param docRoot       Apache document root
     * @param projectName   Project name
     * @param ifDoesNotExit Success flag to return if the project does not exist
     * @return Success (true or false)
     */
    public static boolean backupProject( final File docRoot, final String projectName, Boolean ifDoesNotExit ) {
        File projectRoot = new File( docRoot, "sims/" + projectName );
        if ( !projectRoot.exists() ) {
            logger.warn( "Unable to backup project with name: " + projectName + ", cannot find project root" );
            return ifDoesNotExit;
        }

        ProjectPropertiesFile props = getProjectPropertiesFile( docRoot, projectName );

        String versionString = props.getMajorVersionString() + "." + props.getMinorVersionString() + "." + props.getDevVersionString() + "-r" + props.getSVNVersion();
        DateFormat format = new SimpleDateFormat( "EEE_dd_MMM_yyyy" );
        Date date = new Date();
        String dateString = date.getTime() + "-" + format.format( date );
        File backupDir = new File( docRoot, "website-backup/sims/" + projectName + "-" + versionString + "-" + dateString );

        logger.info( "backing up project " + projectName + " to " + backupDir.getAbsolutePath() );

        backupDir.mkdirs();

        try {
            FileUtils.copyRecursive( projectRoot, backupDir );
        }
        catch( IOException e ) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Synchronizes the in-database projects, simulations, and translations to correspond to the files in the project
     * directory
     *
     * @param docRoot     The document root
     * @param session     A Hibernate session (preferably the one in the request cycle, but you can pull one out of other
     *                    sources)
     * @param projectName The project name to synchronize
     */
    public static void synchronizeProject( final File docRoot, final Session session, final String projectName ) {
        synchronizeProject( docRoot, session, projectName, defaultSyncLogger );
    }

    /**
     * Synchronizes the in-database projects, simulations, and translations to correspond to the files in the project
     * directory
     *
     * @param docRoot     The document root
     * @param session     A Hibernate session (preferably the one in the request cycle, but you can pull one out of other
     *                    sources)
     * @param projectName The project name to synchronize
     * @param syncLogger  The logger to output messages to
     */
    public static void synchronizeProject( final File docRoot, final Session session, final String projectName, final Logger syncLogger ) {

        syncLogger.info( "Synchronizing project " + projectName + " with docroot " + docRoot.getAbsolutePath() );

        // add in simulation descriptions and learning goals
        final HashMap<String, String> englishStringsToAdd = new HashMap<String, String>();

        // wrap it in a transaction for exception handling. this should properly roll back anything that goes bad, and will
        // log any exceptions
        boolean success = HibernateUtils.wrapTransaction( session, new HibernateTask() {
            public boolean run( Session session ) {
                try {
                    boolean projectDirty = false; // whether things are actually getting updated. will stay false if nothing changed

                    File projectRoot = new File( docRoot, "sims/" + projectName );
                    if ( !projectRoot.exists() ) {
                        syncLogger.warn( "Project root " + projectRoot.getAbsolutePath() + " for project " + projectName + " does not exist" );
                        return false;
                    }

//                    boolean hasSWF = ( new File( projectRoot, projectName + ".swf" ) ).exists();
                    // this is changed to check for ANY existance of a SWF. If a SWF exists, we are assuming the project is a Flash/Flex project
                    boolean hasSWF = projectRoot.listFiles( new FilenameFilter() {
                        public boolean accept( File file, String s ) {
                            return s.endsWith( ".swf" );
                        }
                    } ).length > 0;

                    int type;
                    String debugType;
                    if ( projectRoot.getAbsolutePath().contains( "sims/html" ) ) {
                        type = TYPE_HTML;
                        debugType = "HTML";
                    }
                    else {
                        type = hasSWF ? TYPE_FLASH : TYPE_JAVA;
                        debugType = hasSWF ? "flash" : "java";
                    }

                    syncLogger.debug( "detecting project with root" + projectRoot + " type as: " + debugType );

                    List plist = session.createQuery( "select p from Project as p where p.name = :name" ).setString( "name", projectName ).list();
                    if ( plist.size() > 1 ) {
                        throw new RuntimeException( "Multiple projects per one name? BAD THINGS! Fix that database!" );
                    }
                    boolean projectExisted = plist.size() == 1;

                    Project project;
                    if ( projectExisted ) {
                        syncLogger.debug( "Project already exists" );
                        project = (Project) plist.get( 0 );
                    }
                    else {
                        syncLogger.info( "Project doesn't exit. Creating and setting visibility to false" );
                        project = new Project();
                        project.setName( projectName );
                        project.setVisible( false );
                        project.setType( type );
                        projectDirty = true;
                    }

                    if ( type == TYPE_HTML ) {
                        File latestHTMLVersionDirectory = getLatestHTMLDirectory( projectRoot );
                        String[] versionNumbers = latestHTMLVersionDirectory.getName().split( "\\." );
                        try {
                            project.setVersionMajor( Integer.parseInt( versionNumbers[0] ) );
                            project.setVersionMinor( Integer.parseInt( versionNumbers[1] ) );
                            project.setVersionDev( Integer.parseInt( versionNumbers[2] ) );
                        }
                        catch ( NumberFormatException e ) {
                            logger.warn( "Number format exception parsing directory name: " + latestHTMLVersionDirectory.getName() );
                        }
                        project.setVersionRevision( 0 );
                        project.setVersionTimestamp( 0 );
                    }
                    else {
                        ProjectPropertiesFile projectProperties = project.getProjectPropertiesFile( docRoot );
                        if ( project.versionMajor != projectProperties.getMajorVersion() ) {
                            syncLogger.info( "Updating Major to " + projectProperties.getMajorVersion() );
                            project.setVersionMajor( projectProperties.getMajorVersion() );
                            projectDirty = true;
                        }
                        if ( project.versionMinor != projectProperties.getMinorVersion() ) {
                            syncLogger.info( "Updating Minor to " + projectProperties.getMinorVersion() );
                            project.setVersionMinor( projectProperties.getMinorVersion() );
                            projectDirty = true;
                        }
                        if ( project.versionDev != projectProperties.getDevVersion() ) {
                            syncLogger.info( "Updating Dev to " + projectProperties.getDevVersion() );
                            project.setVersionDev( projectProperties.getDevVersion() );
                            projectDirty = true;
                        }
                        if ( project.versionRevision != projectProperties.getSVNVersion() ) {
                            syncLogger.info( "Updating Revision to " + projectProperties.getSVNVersion() );
                            project.setVersionRevision( projectProperties.getSVNVersion() );
                            projectDirty = true;
                        }
                        if ( project.versionTimestamp != projectProperties.getVersionTimestamp() ) {
                            syncLogger.info( "Updating Timestamp to " + projectProperties.getVersionTimestamp() );
                            project.setVersionTimestamp( projectProperties.getVersionTimestamp() );
                            projectDirty = true;
                        }
                    }

                    // used so we know which simulations we have already encountered
                    Map<String, Simulation> simulationCache = new HashMap<String, Simulation>();

                    // used for session save / update at the end
                    List<Simulation> nonCreatedSims = new LinkedList<Simulation>();
                    List<Simulation> createdSims = new LinkedList<Simulation>();
                    List<LocalizedSimulation> modifiedLSims = new LinkedList<LocalizedSimulation>();
                    List<LocalizedSimulation> createdLSims = new LinkedList<LocalizedSimulation>();

                    // so that we know which simulations we haven't found
                    Set<Simulation> missedSimulations = new HashSet<Simulation>( project.getSimulations() );
                    Set<LocalizedSimulation> missedLocalizedSimulations = new HashSet<LocalizedSimulation>();

                    Document document;
                    if ( type == TYPE_HTML ) {
                        try {
                            String unqualifiedName = projectName.substring( 5 );
                            try {
                                document = XMLUtils.toDocument( FileUtils.loadFileAsString( new File( projectRoot, unqualifiedName + ".xml" ) ) );
                            }
                            catch ( FileNotFoundException e ) {
                                logger.warn( "file " + unqualifiedName + " not found" );
                                document = null;
                            }
                        }
                        catch ( StringIndexOutOfBoundsException e ) {
                            logger.error( "project name: '" + projectName + "' has type html but no html/ prefix" );
                            document = null;
                        }
                    }
                    else {
                        document = XMLUtils.toDocument( FileUtils.loadFileAsString( new File( projectRoot, projectName + ".xml" ) ) );
                    }

                    if (document != null ) {
                        NodeList simulationNodes = document.getElementsByTagName( "simulation" );

                        for ( int i = 0; i < simulationNodes.getLength(); i++ ) {
                            Element element = (Element) simulationNodes.item( i );

                            String simName = element.getAttribute( "name" );
                            String simLocaleString = element.getAttribute( "locale" );
                            Node titleChild = element.getElementsByTagName( "title" ).item( 0 );

                            // need to check for the degenerate case where the title was translated to the empty string.
                            String simTitle;
                            if ( titleChild != null && titleChild.getChildNodes().getLength() > 0 ) {
                                simTitle = titleChild.getChildNodes().item( 0 ).getNodeValue();
                            }
                            else {
                                // if there is no title, reset to the simulation name
                                simTitle = simName;
                            }


                            Locale simLocale = LocaleUtils.stringToLocale( simLocaleString );

                            syncLogger.debug( "Reading localized simulation XML for: " + simName + " - " + simLocaleString + " - " + simTitle );

                            if ( !project.getSimulationJARFile( docRoot, simName, simLocale ).exists() &&
                                 !project.getSimulationHTMLFile( docRoot, simName, simLocale ).exists() ) {
                                syncLogger.warn( "Project: " + projectName + " sim: " + simName + ", locale: " + simLocaleString );
                                syncLogger.warn( "Simulation JAR or HTML file does not exist for specified XML entry. Most likely not deployed yet" );
                                syncLogger.warn( "Skipping" );
                                continue;
                            }

                            // pick or create the simulation
                            Simulation simulation;
                            if ( simulationCache.containsKey( simName ) ) {
                                // we've already obtained the simulation
                                simulation = simulationCache.get( simName );
                            }
                            else {
                                // we need to either grab or create the simulation
                                List slist = session.createQuery( "select s from Simulation as s where s.name = :name" ).setString( "name", simName ).list();
                                if ( slist.size() > 1 ) {
                                    throw new RuntimeException( "Multiple simulations per one name? BAD THINGS! Fix that database!" );
                                }
                                if ( slist.isEmpty() ) {
                                    syncLogger.info( "Cannot find a simulation for " + simName + ", will create one" );
                                    simulation = new Simulation();
                                    simulation.setName( simName );
                                    simulation.setProject( project );
                                    simulation.setDesignTeam( "" );
                                    simulation.setLibraries( "" );
                                    simulation.setThanksTo( "" );
                                    simulation.setUnderConstruction( false );
                                    simulation.setGuidanceRecommended( false );
                                    simulation.setClassroomTested( false );
                                    simulation.setSimulationVisible( false );
                                    simulation.setHasCreativeCommonsAttributionLicense( true ); // new sims should generally have this set
                                    simulation.setFaqVisible( false );
                                    simulation.setLowGradeLevel( GradeLevel.ELEMENTARY_SCHOOL );
                                    simulation.setHighGradeLevel( GradeLevel.UNIVERSITY );
                                    createdSims.add( simulation );
                                    simulationCache.put( simName, simulation );
                                    englishStringsToAdd.put( simulation.getDescriptionKey(), Simulation.DEFAULT_DESCRIPTION );
                                    englishStringsToAdd.put( simulation.getLearningGoalsKey(), Simulation.DEFAULT_LEARNING_GOALS );
                                    projectDirty = true;
                                }
                                else {
                                    simulation = (Simulation) slist.get( 0 );
                                    simulationCache.put( simName, simulation );
                                    missedSimulations.remove( simulation );
                                    nonCreatedSims.add( simulation );
                                    syncLogger.debug( "Found simulation " + simulation.getName() );
                                    if ( simulation.getProject().getId() != project.getId() ) {
                                        syncLogger.warn( "Found simulation " + simulation.getName() + " specified with a different project " + simulation.getProject().getName() + " instead of " + project.getName() + "." );
                                        syncLogger.warn( "Modifying to match the current project (with type)" );
                                        syncLogger.warn( "This may be caused by creating a new simulation. If so, ignore the above two messages" );
                                        simulation.setProject( project );
                                    }
                                    missedLocalizedSimulations.addAll( simulation.getLocalizedSimulations() );
                                }
                            }

                            simulation.setKilobytes( simulation.detectSimKilobytes( docRoot ) );

                            List llist = new LinkedList();
                            if ( !createdSims.contains( simulation ) ) {
                                llist = session.createQuery( "select ls from LocalizedSimulation as ls where ls.locale = :locale and ls.simulation = :simulation" )
                                        .setLocale( "locale", simLocale ).setEntity( "simulation", simulation ).list();
                            }
                            if ( llist.size() > 1 ) {
                                throw new RuntimeException( "Multiple localized simulations per one locale and simulation? BAD THINGS! Fix that database!" );
                            }

                            if ( llist.isEmpty() ) {
                                syncLogger.info( "Creating lsim for " + simulation.getName() + " with locale " + simLocaleString + " and title " + simTitle );
                                LocalizedSimulation lsim = new LocalizedSimulation();
                                createdLSims.add( lsim );
                                lsim.setSimulation( simulation );
                                lsim.setTitle( simTitle );
                                lsim.setLocale( simLocale );
                                projectDirty = true;
                            }
                            else {
                                LocalizedSimulation lsim = (LocalizedSimulation) llist.get( 0 );
                                missedLocalizedSimulations.remove( lsim );
                                if ( !lsim.getTitle().equals( simTitle ) ) {
                                    syncLogger.info( "Changing lsim title for " + lsim.getSimulation().getName() + " and locale " + simLocaleString + " from '" + lsim.getTitle() + "' to '" + simTitle + "'" );
                                    lsim.setTitle( simTitle );
                                    modifiedLSims.add( lsim );
                                    projectDirty = true;
                                }
                            }

                        }
                    }

                    for ( Simulation simulation : missedSimulations ) {
                        logger.warn( "Did not have XML information for simulation " + simulation.getName() + " that is already within the project" );
                    }

                    if ( !missedSimulations.isEmpty() ) {
                        logger.warn( "Maybe these simulations were deleted in the repository, or renamed?" );
                        logger.warn( "Manual changes will be needed to remove the old version, including deleting references to the simulations first" );
                    }

                    for ( LocalizedSimulation localizedSimulation : missedLocalizedSimulations ) {
                        logger.warn( "Did not have XML information for localized simulation " + localizedSimulation.getSimulation().getName() + ":" + localizedSimulation.getLocaleString() + "." );
                    }

                    if ( !missedLocalizedSimulations.isEmpty() ) {
                        logger.warn( "Maybe these translations were deleted in the repository? Manual modifications or deletions are necessary" );
                        logger.warn( "Visit the simulation page(s) to manually remove translations" );
                    }

                    Date currentTime = new Date();

                    // save the project
                    if ( projectExisted ) {
                        session.update( project );
                    }
                    else {
                        session.save( project );
                    }

                    for ( Simulation sim : nonCreatedSims ) {
                        if ( projectDirty ) { // only set the update time if we made modifications
                            sim.setUpdateTime( currentTime );
                        }
                        session.update( sim );
                    }
                    for ( LocalizedSimulation lsim : modifiedLSims ) {
                        session.update( lsim );
                    }
                    for ( Simulation sim : createdSims ) {
                        //sim.setCreateTime( currentTime ); actually don't set creation time. we wait until the sim is made visible to do this
                        sim.setUpdateTime( currentTime );
                        session.save( sim );
                    }
                    for ( LocalizedSimulation lsim : createdLSims ) {
                        session.save( lsim );
                    }
                }
                catch ( TransformerException e ) {
                    e.printStackTrace();
                    return false;
                }
                catch ( ParserConfigurationException e ) {
                    e.printStackTrace();
                    return false;
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            }
        } );
        if ( success ) {
            // add in English strings for simulations, but ONLY IF THEY DO NOT EXIST!
            for ( String key : englishStringsToAdd.keySet() ) {
                StringUtils.addString( session, key, englishStringsToAdd.get( key ) );
            }
        }
    }

    /**
     * Returns a string with information about project consistency between the database representation and the files in
     * the deployment directory
     * <p/>
     * NOTE: should be persistent when this is called.
     *
     * @param docRoot Document root file
     * @return Information string
     */
    public String consistencyCheck( File docRoot ) {
        StringBuilder builder = new StringBuilder();
        builder.append( "project: <font color='#0000FF'>" + name + "</font>" );

        File projectRoot = new File( docRoot, "sims/" + name );
        ProjectPropertiesFile projectProperties = getProjectPropertiesFile( docRoot );

        boolean warning = false;

        if ( projectProperties.exists() ) {
            if ( projectProperties.getMajorVersion() != versionMajor ) {
                appendWarning( builder, "Major version mismatch! properties: " + projectProperties.getMajorVersion() + " db: " + versionMajor );
                warning = true;
            }
            if ( projectProperties.getMinorVersion() != versionMinor ) {
                appendWarning( builder, "Minor version mismatch! properties: " + projectProperties.getMinorVersion() + " db: " + versionMinor );
                warning = true;
            }
            if ( projectProperties.getDevVersion() != versionDev ) {
                appendWarning( builder, "Dev version mismatch! properties: " + projectProperties.getDevVersion() + " db: " + versionDev );
                warning = true;
            }
            if ( projectProperties.getSVNVersion() != versionRevision ) {
                appendWarning( builder, "revision mismatch! properties: " + projectProperties.getSVNVersion() + " db: " + versionRevision );
                warning = true;
            }
            if ( projectProperties.getVersionTimestamp() != versionTimestamp ) {
                appendWarning( builder, "Timestamp version mismatch! properties: " + projectProperties.getVersionTimestamp() + " db: " + versionTimestamp );
                warning = true;
            }

            try {
                Document document = XMLUtils.toDocument( FileUtils.loadFileAsString( new File( projectRoot, name + ".xml" ) ) );

                NodeList simulations = document.getElementsByTagName( "simulation" );

                Set<Simulation> usedSims = new HashSet<Simulation>();
                Set<LocalizedSimulation> usedLSims = new HashSet<LocalizedSimulation>();

                for ( int i = 0; i < simulations.getLength(); i++ ) {
                    Element element = (Element) simulations.item( i );

                    String simName = element.getAttribute( "name" );
                    String simLocaleString = element.getAttribute( "locale" );
                    Node titleChild = element.getElementsByTagName( "title" ).item( 0 );

                    // need to check for the degenerate case where the title was translated to the empty string.
                    String simTitle;
                    if ( titleChild != null && titleChild.getChildNodes().getLength() > 0 ) {
                        simTitle = titleChild.getChildNodes().item( 0 ).getNodeValue();
                    }
                    else {
                        // if there is no title, reset to the simulation name
                        simTitle = simName;
                    }
                    //String simTitle = element.getElementsByTagName( "title" ).item( 0 ).getChildNodes().item( 0 ).getNodeValue();

                    Locale simLocale = LocaleUtils.stringToLocale( simLocaleString );

                    File simulationJAR = getSimulationJARFile( docRoot, simName, simLocale );
                    if ( !simulationJAR.exists() ) {
                        builder.append( "<br/>" + simulationJAR.getName() + " does not exist" );
                        continue;
                    }

                    Simulation sim = null;
                    LocalizedSimulation lsim = null;

                    for ( Object o : getSimulations() ) {
                        Simulation so = (Simulation) o;
                        if ( so.getName().equals( simName ) ) {
                            sim = so;
                            usedSims.add( so );
                            break;
                        }
                    }

                    if ( sim != null ) {
                        for ( Object o : sim.getLocalizedSimulations() ) {
                            LocalizedSimulation lo = (LocalizedSimulation) o;
                            if ( lo.getLocale().equals( simLocale ) ) {
                                lsim = lo;
                                usedLSims.add( lo );
                                break;
                            }
                        }

                        if ( lsim != null ) {
                            if ( !lsim.getTitle().equals( simTitle ) ) {
                                appendWarning( builder, "Sim title changed? (" + simName + ", " + simLocaleString + ") xml: " + simTitle + " db: " + lsim.getTitle() );
                                warning = true;
                            }
                        }
                        else {
                            appendWarning( builder, "Could not find translation " + simLocaleString + " for simulation " + simName );
                            warning = true;
                        }
                    }
                    else {
                        appendWarning( builder, "Could not find simulation " + simName + " in the DB" );
                        warning = true;
                    }
                }

                for ( Object o : getSimulations() ) {
                    Simulation sim = (Simulation) o;
                    if ( !usedSims.contains( sim ) ) {
                        appendWarning( builder, "Found sim " + sim.getName() + " in database, but did not find in project directory" );
                        warning = true;
                    }
                    else {
                        for ( Object o1 : sim.getLocalizedSimulations() ) {
                            LocalizedSimulation lsim = (LocalizedSimulation) o1;
                            if ( !usedLSims.contains( lsim ) ) {
                                appendWarning( builder, "Found translation " + lsim.getLocaleString() + " for sim " + sim.getName() + " in database, but did not find in project directory" );
                                warning = true;
                            }
                        }
                    }
                }
            }
            catch ( Exception e ) {
                e.printStackTrace();
                logger.warn( "Error matching XML and simulations", e );
                appendWarning( builder, "Error matching XML and simulations" );
                warning = true;
            }
        }
        else {
            appendWarning( builder, "Could not find project properties file" );
            warning = true;
        }

        for ( Object o : getSimulations() ) {
            Simulation sim = (Simulation) o;
            int detected = sim.detectSimKilobytes( docRoot );
            if ( sim.getKilobytes() != detected ) {
                appendWarning( builder, "Sim " + sim.getName() + " kilobytes inaccurate. file: " + detected + " db: " + sim.getKilobytes() );
                warning = true;
            }
        }

        if ( !warning ) {
            builder.append( " <font color='#00FF00'>OK</font><br/>" );
        }
        else {
            builder.append( "<br/>" );
        }

        return builder.toString();
    }

    public static File getLatestHTMLDirectory( File projectRoot ) {
        if ( !projectRoot.getAbsolutePath().contains( "sims/html" ) ) {
            return null;
        }
        File[] htmlVersionDirectories = projectRoot.listFiles( new FilenameFilter() {
            public boolean accept( File file, String s ) {
                return s.matches( "\\d+\\.\\d+\\.\\d+" );
            }
        } );
        Arrays.sort( htmlVersionDirectories );
        if ( htmlVersionDirectories.length > 0 ) {
            return htmlVersionDirectories[htmlVersionDirectories.length - 1];
        }
        else {
            return null;
        }
    }

    public boolean isJava() {
        return getType() == 0;
    }

    public boolean isFlash() {
        return getType() == 1;
    }

    public boolean isHTML() {
        return getType() == 2;
    }

    // getters and setters

    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType( int type ) {
        this.type = type;
    }

    public int getVersionMajor() {
        return versionMajor;
    }

    public void setVersionMajor( int versionMajor ) {
        this.versionMajor = versionMajor;
    }

    public int getVersionMinor() {
        return versionMinor;
    }

    public void setVersionMinor( int versionMinor ) {
        this.versionMinor = versionMinor;
    }

    public int getVersionDev() {
        return versionDev;
    }

    public void setVersionDev( int versionDev ) {
        this.versionDev = versionDev;
    }

    public int getVersionRevision() {
        return versionRevision;
    }

    public void setVersionRevision( int versionRevision ) {
        this.versionRevision = versionRevision;
    }

    public long getVersionTimestamp() {
        return versionTimestamp;
    }

    public void setVersionTimestamp( long versionTimestamp ) {
        this.versionTimestamp = versionTimestamp;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible( boolean visible ) {
        this.visible = visible;
    }

    public Set getSimulations() {
        return simulations;
    }

    public void setSimulations( Set simulations ) {
        this.simulations = simulations;
    }
}
