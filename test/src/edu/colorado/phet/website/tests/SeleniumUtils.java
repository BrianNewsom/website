/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import org.w3c.dom.Document;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.flashlauncher.util.XMLUtils;
import edu.colorado.phet.website.constants.WebsiteConstants;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public class SeleniumUtils {
    public static final String WEB_SERVER = "localhost";
    public static final int PORT = 4444;

    public static Selenium createDefaultSelenium() {
        return new DefaultSelenium( "localhost", SeleniumUtils.PORT, "*chrome", "http://" + SeleniumUtils.WEB_SERVER + "/" );
    }

    public static String getString( String key ) {
        return getString( key, new Locale( "en" ) );
    }

    public static String getString( String key, Locale locale ) {
        try {
            URL url = new URL( "http://" + WEB_SERVER + "/services/get-string?key=" + key + "&locale=" + LocaleUtils.localeToString( locale ) );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod( "GET" );
            conn.setDoInput( true );

            BufferedReader reader = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );
            String line = reader.readLine();

            Document document = XMLUtils.toDocument( line );
            if ( document.getFirstChild().getNodeName().equals( "error" ) ) {
                return null;
            }
            return document.getFirstChild().getTextContent();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main( String[] args ) {
        System.out.println( "String: " + getString( "language.dir", WebsiteConstants.ENGLISH ) );
    }
}
