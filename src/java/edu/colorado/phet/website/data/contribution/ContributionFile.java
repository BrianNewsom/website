/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.data.contribution;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.data.util.IntId;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.links.AbstractLinker;

public class ContributionFile implements Serializable, IntId {

    /**
     * File types that are allowed to be uploaded. Older ones are grandfathered in, but currently not gracefully.
     * (contributions with non-compliant files cannot be updated)
     */
    public static final String[] FILE_TYPE_WHITELIST = new String[] { "pdf", "doc", "docx", "ppt", "xls", "txt", "pptx", "xlsx", "cck", "esp", "notebook" };

    private int id;
    private Contribution contribution;
    private String filename;
    private int size; // bytes

    private int oldId;

    private static final Logger logger = Logger.getLogger( ContributionFile.class.getName() );

    public ContributionFile() {
    }

    /*---------------------------------------------------------------------------*
    * public methods
    *----------------------------------------------------------------------------*/

    public File getFileLocation() {
        return new File( PhetWicketApplication.get().getActivitiesRoot(), getRelativeLocation() );
    }

    public String getRelativeLocation() {
        return getRelativeLocation( String.valueOf( contribution.getId() ) );
    }

    public String getRelativeLocation( String id ) {
        // added a few sanity checks on filename to prevent files being written lower down
        return id + "/" + filename.replace( '/', '_' ).replace( '\\', '_' );
    }

    // NOTE: should only be delivered from the contribution pages, or the installer will need to be changed
    public AbstractLinker getLinker() {
        return new AbstractLinker() {
            @Override
            public String getRawUrl( PageContext context, PhetRequestCycle cycle ) {
                PhetWicketApplication app = PhetWicketApplication.get();
                URI uri = new File( "/" + getRelativeLocation() ).toURI();
                String uriString = uri.toString();
                if ( uriString.startsWith( "file:" ) ) {
                    uriString = uriString.substring( "file:".length() );
                }
                return app.getActivitiesLocation() + uriString;
            }

            @Override
            public String getSubUrl( PageContext context ) {
                return null;
            }
        };
    }

    public static boolean validateFileExtension( String filename ) {
        if ( filename.indexOf( "." ) < 0 ) {
            return true;
        }
        for ( String s : FILE_TYPE_WHITELIST ) {
            if ( filename.endsWith( "." + s ) || filename.endsWith( "." + s.toUpperCase() ) ) {
                return true;
            }
        }
        return false;
    }

    public static void orderFiles( List<ContributionFile> files ) {
        Collections.sort( files, new Comparator<ContributionFile>() {
            public int compare( ContributionFile a, ContributionFile b ) {
                return a.getFilename().compareToIgnoreCase( b.getFilename() );
            }
        } );
    }

    public static void orderFilesCast( List files ) {
        Collections.sort( files, new Comparator() {
            public int compare( Object a, Object b ) {
                return ( (ContributionFile) a ).getFilename().compareToIgnoreCase( ( (ContributionFile) b ).getFilename() );
            }
        } );
    }

    /**
     * Used for transferring files from the old data
     *
     * @param id Contribution file ID
     * @return
     */
    public File getTmpFileLocation( String id ) {
        return new File( PhetWicketApplication.get().getActivitiesRoot(), "tmp" + getRelativeLocation( id ) );
    }

    /**
     * Retrieve a list of recommended filetypes, using the file separator in the style and locale of the component.
     * The logic for file extension allowance is defined in validateFileExtension()
     *
     * @param component The component
     * @return List of file extensions
     */
    public static String getFiletypes( Component component ) {
        String separator = StringUtils.getSeparator( component );
        StringBuilder ret = new StringBuilder();
        boolean started = false;
        for ( String fileExtension : ContributionFile.FILE_TYPE_WHITELIST ) {
            if ( started ) {
                ret.append( separator );
            }
            started = true;
            ret.append( fileExtension );
        }
        return ret.toString();
    }

    /*---------------------------------------------------------------------------*
    * getters and setters
    *----------------------------------------------------------------------------*/

    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }

    public Contribution getContribution() {
        return contribution;
    }

    public void setContribution( Contribution contribution ) {
        this.contribution = contribution;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename( String filename ) {
        this.filename = filename;
    }

    public int getSize() {
        return size;
    }

    public void setSize( int size ) {
        this.size = size;
    }

    public int getOldId() {
        return oldId;
    }

    public void setOldId( int oldId ) {
        this.oldId = oldId;
    }
}