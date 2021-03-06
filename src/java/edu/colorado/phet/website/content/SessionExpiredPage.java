/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content;

import org.apache.wicket.PageParameters;

import edu.colorado.phet.website.components.LocalizedText;
import edu.colorado.phet.website.templates.PhetMenuPage;
import edu.colorado.phet.website.util.PhetUrlMapper;

public class SessionExpiredPage extends PhetMenuPage {
    public SessionExpiredPage( PageParameters parameters ) {
        super( parameters );

        setTitle( getLocalizer().getString( "error.sessionExpired", this ) );

        add( new LocalizedText( "message", "error.sessionExpired.message" ) );

        hideSocialBookmarkButtons();
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public boolean isErrorPage() {
        return true;
    }

    public static void addToMapper( PhetUrlMapper mapper ) {
//        mapper.addMap( "^$", SessionExpiredPage.class );
    }
}