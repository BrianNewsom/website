/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.admin.deploy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import edu.colorado.phet.buildtools.JARGenerator;
import edu.colorado.phet.common.phetcommon.util.FileUtils;
import edu.colorado.phet.website.data.Project;

/**
 * Publishes a translation deployment from the temporary dir to the live sim dir
 */
public class WebsiteTranslationDeployPublisher {

    private File sims;
    private File docRoot;

    private List<String> deployedProjectNames = new LinkedList<String>();

    private static final Logger logger = Logger.getLogger( WebsiteTranslationDeployPublisher.class.getName() );

    public WebsiteTranslationDeployPublisher( File sims, File docRoot ) {
        this.sims = sims;
        this.docRoot = docRoot;
    }

    public void publishTranslations( File translationDir ) throws IOException {

        /*---------------------------------------------------------------------------*
        * java projects
        *----------------------------------------------------------------------------*/
        ArrayList<String> javaProjectNameList = WebsiteTranslationDeployServer.getJavaProjectNameList( translationDir );
        for ( String project : javaProjectNameList ) {
            logger.info( "backing up " + project );
            Project.backupProject( docRoot, project );
            String[] locales = WebsiteTranslationDeployServer.getJavaTranslatedLocales( translationDir, project );

            // if it doesn't include the English translation, include it
            boolean containsEnglish = containsEnglish( locales );
            if ( !containsEnglish ) {
                String[] oldLocales = locales;
                locales = new String[locales.length + 1];
                System.arraycopy( oldLocales, 0, locales, 0, oldLocales.length );
                locales[oldLocales.length] = "en";
            }

            // remove packed JAR files since our process does not guarantee it will be produced (does not halt on failure).
            // we don't want to serve a stale pack200 JAR!
            removeProjectPackedJar( project );

            copyToSimsDir( translationDir, project, locales );
            generateJNLPs( translationDir, project, locales );
        }

        /*---------------------------------------------------------------------------*
        * flash projects
        *----------------------------------------------------------------------------*/
        ArrayList<String> flashProjectNameList = WebsiteTranslationDeployServer.getFlashProjectNameList( translationDir );
        for ( String project : flashProjectNameList ) {
            if ( project.equals( "common" ) ) {
                logger.info( "Not publishing common strings XML directly" );
                continue;
            }

            logger.info( "backing up " + project );
            Project.backupProject( docRoot, project );

            String[] locales = WebsiteTranslationDeployServer.getFlashTranslatedLocales( translationDir, project );

            copyFlashFiles( translationDir, project, locales );
        }

        copyMetaXML( translationDir, javaProjectNameList );
        copyMetaXML( translationDir, flashProjectNameList );

        deployedProjectNames.addAll( javaProjectNameList );
        deployedProjectNames.addAll( flashProjectNameList );

    }

    private boolean containsEnglish( String[] locales ) {
        boolean containsEnglish = false;
        for ( String locale : locales ) {
            if ( locale.equals( "en" ) ) {
                containsEnglish = true;
                break;
            }
        }
        return containsEnglish;
    }

    public static Logger getLogger() {
        return logger;
    }

    public List<String> getDeployedProjectNames() {
        return deployedProjectNames;
    }

    private void copyMetaXML( File translationDir, ArrayList projectNameList ) {
        for ( Object ob : projectNameList ) {
            String project = (String) ob;
            try {
                FileUtils.copyToDir( new File( translationDir, project + ".xml" ), new File( sims, project ) );
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * copy the necessary flash HTML files in translationDir (with project and locales) to the main location
     */
    private void copyFlashFiles( File translationDir, String project, String[] locales ) {
        for ( String simName : WebsiteTranslationDeployServer.getFlashSimulations( translationDir, project ) ) {
            for ( String localeString : locales ) {
                try {
                    // copy the JAR
                    String JARName = simName + "_" + localeString + ".jar";
                    File fromJARFile = new File( translationDir, JARName );
                    File toJARFile = new File( sims, project + "/" + JARName );
                    FileUtils.copyTo( fromJARFile, toJARFile );

                    // copy the HTML
                    String HTMLName = simName + "_" + localeString + ".html";
                    File fromHTMLFile = new File( translationDir, HTMLName );
                    File toHTMLFile = new File( sims, project + "/" + HTMLName );
                    FileUtils.copyTo( fromHTMLFile, toHTMLFile );
                }
                catch ( IOException e ) {
                    e.printStackTrace();

                    // this should be a failure point
                    throw new RuntimeException( e );
                }
            }

            // also attempt to copy over the English JARs that have the added translation(s) poked in
            String englishJarName = simName + "_en.jar";
            try {
                FileUtils.copyTo( new File( translationDir, englishJarName ), new File( sims, project + "/" + englishJarName ) );
            }
            catch ( IOException e ) {
                logger.warn( "was unable to copy english JAR " + englishJarName );
            }
        }
    }

    private void removeProjectPackedJar( String project ) throws IOException {
        File packedJarFile = getPackedAllJAR( new File( sims, project ), project );
        if ( packedJarFile.exists() ) {
            logger.info( "removing packed JAR file for project " + project );
            packedJarFile.delete();
        }
    }

    /**
     * copy new translated JARs and project_all.jar to the sims directory
     */
    private void copyToSimsDir( File translationDir, String project, String[] locales ) throws IOException {
        String[] flavors = JARGenerator.getFlavors( getAllJAR( translationDir, project ) );
        for ( String flavor : flavors ) {
            for ( String localeString : locales ) {
                File jar = new File( translationDir, flavor + "_" + localeString + ".jar" );
                FileUtils.copyToDir( jar, new File( sims, project ) );
            }
        }
        FileUtils.copyToDir( getAllJAR( translationDir, project ), new File( sims, project ) );
        File packedAllJAR = getPackedAllJAR( translationDir, project );
        if ( packedAllJAR.exists() ) {
            FileUtils.copyToDir( packedAllJAR, new File( sims, project ) );
        }
        else {
            logger.warn( "packed _all.jar not found: " + packedAllJAR.getAbsolutePath() );
        }
    }

    private File getAllJAR( File translationDir, String project ) {
        return new File( translationDir, project + "_all.jar" );
    }

    public static File getPackedAllJAR( File translationDir, String project ) {
        return new File( translationDir, project + "_all.jar.pack.gz" );
    }

    //generate new JNLPs in sims directory
    private void generateJNLPs( File translationDir, String project, String[] locales ) throws IOException {
        //TODO generate fresh JNLPs instead?
        //might be safer to copy existing JNLP to make sure main class is right, etc.

        String[] flavors = JARGenerator.getFlavors( getAllJAR( translationDir, project ) );
        for ( String flavor : flavors ) {
            //for now, copy english JNLP and replace value="en" with value="locale_STR"
            //also fix codebase
            String englishJNLP = FileUtils.loadFileAsString( new File( sims, project + "/" + flavor + "_en.jnlp" ), "UTF-8" );
            for ( String localeString : locales ) {
                StringTokenizer stringTokenizer = new StringTokenizer( localeString, "_ " );
                String language = stringTokenizer.nextToken();
                String country = stringTokenizer.hasMoreTokens() ? stringTokenizer.nextToken() : null;

                String replacement = "<property name=\"javaws.user.language\" value=\"" + language + "\" />";
                if ( country != null ) {
                    replacement = replacement + "<property name=\"javaws.user.country\" value=\"" + country + "\" />";
                }
                String out = FileUtils.replaceFirst( englishJNLP, "<property name=\"javaws.user.language\" value=\"en\" />", replacement );

                out = FileUtils.replaceAll( out, "href=\"" + flavor + "_en.jnlp\"", "href=\"" + flavor + "_" + localeString + ".jnlp\"" );
                FileUtils.writeString( new File( sims, project + "/" + flavor + "_" + localeString + ".jnlp" ), out, "UTF-8" );
            }
        }
    }

}