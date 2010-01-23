package edu.colorado.phet.website.authentication;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.request.target.basic.RedirectRequestTarget;

import edu.colorado.phet.website.content.IndexPage;
import edu.colorado.phet.website.templates.PhetPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.Linkable;

public class SignOutPage extends PhetPage {

    // TODO: i18nize

    private static Logger logger = Logger.getLogger( SignOutPage.class.getName() );

    public SignOutPage( PageParameters parameters ) {
        super( parameters, true );

        // TODO: i18nize
        addTitle( "Logging out" );

        AuthenticatedPage.checkSignedIn();

        PhetSession.get().signOut();

        // redirect the user to the home page
        getRequestCycle().setRequestTarget( new RedirectRequestTarget( "/" ) );
    }

    public static Linkable getLinker() {
        return new AbstractLinker() {
            @Override
            public String getSubUrl( PageContext context ) {
                return "sign-out";
            }
        };
    }

    public static void addToMapper( PhetUrlMapper mapper ) {
        mapper.addMap( "^sign-out$", SignOutPage.class );
    }


}