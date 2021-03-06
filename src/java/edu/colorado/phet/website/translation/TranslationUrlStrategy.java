/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.PageParameters;
import org.apache.wicket.request.RequestParameters;
import org.apache.wicket.request.target.coding.IRequestTargetUrlCodingStrategy;
import org.apache.wicket.request.target.component.BookmarkablePageRequestTarget;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

public class TranslationUrlStrategy implements IRequestTargetUrlCodingStrategy {
    private String prefix;
    private PhetUrlMapper mapper;

    public static final String FULL_PATH = "fullPath";
    public static final String PATH = "path";
    public static final String LOCALE_STRING = "localeString";
    public static final String PREFIX_STRING = "prefixString";
    public static final String LOCALE = "locale";
    public static final String VARIATION = "variation";
    private static final Logger logger = Logger.getLogger( TranslationUrlStrategy.class.getName() );

    public TranslationUrlStrategy( String prefix, PhetUrlMapper mapper ) {
        this.mapper = mapper;
        this.prefix = prefix;
    }

    public String getMountPath() {
        return prefix;
    }

    public CharSequence encode( IRequestTarget request ) {
        throw new RuntimeException( "TranslationUrlStrategy.encode" );
    }

    public IRequestTarget decode( RequestParameters requestParameters ) {
        logger.debug( "X decode( RequestParameters ): " + requestParameters );
        logger.debug( "X Path: " + requestParameters.getPath() );
        logger.debug( "X ComponentPath: " + requestParameters.getComponentPath() );
        PageParameters params = new PageParameters( requestParameters.getParameters() );

        String basePath = requestParameters.getPath();
        String halfPath = stripPath( basePath );
        String path = stripTranslationId( halfPath );
        int translationId = readTranslationId( halfPath );

        Session session = HibernateUtils.getInstance().openSession();
        Transaction tx = null;
        Locale locale = null;
        try {
            tx = session.beginTransaction();

            locale = (Locale) session.createQuery( "select t.locale from Translation as t where t.id = :id" ).setInteger( "id", translationId ).uniqueResult();

            tx.commit();
        }
        catch ( RuntimeException e ) {
            logger.warn( "Exception: " + e );
            if ( tx != null && tx.isActive() ) {
                try {
                    tx.rollback();
                }
                catch ( HibernateException e1 ) {
                    logger.error( "ERROR: Error rolling back transaction", e1 );
                }
                throw e;
            }
        }
        finally {
            session.close();
        }

        if ( locale == null ) {
            locale = LocaleUtils.stringToLocale( "en" );
        }

        params.add( FULL_PATH, basePath );
        params.add( PATH, path );
        params.add( LOCALE_STRING, LocaleUtils.localeToString( locale ) );
        params.add( PREFIX_STRING, "/" + prefix + "/" + String.valueOf( translationId ) + "/" );
        params.put( LOCALE, locale );
        params.add( VARIATION, String.valueOf( translationId ) );
        Class toClass = mapper.getMappedClass( path, params );
        return new BookmarkablePageRequestTarget( toClass, params );
    }

    private String stripPath( String path ) {
        if ( path.startsWith( "/" + prefix ) ) {
            return path.substring( prefix.length() + 2 );
        }
        else if ( path.startsWith( prefix ) ) {
            return path.substring( prefix.length() + 1 );
        }
        else {
            return path;
        }
    }

    private String stripTranslationId( String path ) {
        int idx = path.indexOf( "/" );
        if ( idx == -1 ) {
            throw new RuntimeException( "Can't find correct translation id in url, bailing because of: " + path );
        }
        return path.substring( idx + 1 );
    }

    private int readTranslationId( String path ) {
        int idx = path.indexOf( "/" );
        if ( idx == -1 ) {
            return -1;
        }
        return Integer.valueOf( path.substring( 0, idx ) );
    }

    public boolean matches( IRequestTarget target ) {
        return mapper.containsClass( target.getClass() );
    }

    public boolean matches( String path, boolean caseSensitive ) {
        if ( caseSensitive ) {
            return matches( path );
        }
        else {
            logger.warn( "matching without case sensitivity on " + path );
            return matches( path.toLowerCase() );
        }
    }

    public boolean matches( String str ) {
        try {
            String halfPath = stripPath( str );
            String path = stripTranslationId( halfPath );
            int translationId = readTranslationId( halfPath );
            logger.debug( "Translated page? id=" + String.valueOf( translationId ) + "\n\tstr=" + str + "\n\thalfPath=" + halfPath + "\n\tpath=" + path );
            Class clazz = mapper.getMappedClass( path );
            boolean ret = clazz != null;
            logger.debug( " XMatches? : " + str + " = " + ret + ( ret ? " for " + clazz.getCanonicalName() : "" ) );
            return ret;
        }
        catch ( RuntimeException e ) {
            logger.warn( e );
            return false;
        }
    }
}
