/*
 * Copyright 2011, University of Colorado
 */

package edu.colorado.phet.website.panels.sponsor;

import org.apache.wicket.markup.html.basic.Label;

import edu.colorado.phet.website.cache.EventDependency;
import edu.colorado.phet.website.components.InvisibleComponent;
import edu.colorado.phet.website.data.TranslatedString;
import edu.colorado.phet.website.data.util.HibernateEventListener;
import edu.colorado.phet.website.data.util.IChangeListener;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;

/**
 * Shows a sponsor highlight on the home page
 */
public class FeaturedSponsorPanel extends PhetPanel {
    public static final String HOME_SPONSOR_STYLE = "border: 1px solid #aaa; background-color: #fff;";

    public FeaturedSponsorPanel( String id, final PageContext context ) {
        super( id, context );

        final Sponsor sponsor = Sponsor.chooseRandomHomeSponsor();
        if ( sponsor.getLogoNeedsSubtitle() ) {
            add( new Label( "featured-sponsor-name", sponsor.getFullName() ) );
        }
        else {
            add( new InvisibleComponent( "featured-sponsor-name" ) );
        }

        add( Sponsor.createSponsorLogoPanel( "featured-sponsor-panel", sponsor, context, HOME_SPONSOR_STYLE ) );

        addDependency( new EventDependency() {

            private IChangeListener stringListener;

            @Override
            protected void addListeners() {
                stringListener = createTranslationChangeInvalidator( context.getLocale() );
                HibernateEventListener.addListener( TranslatedString.class, stringListener );
            }

            @Override
            protected void removeListeners() {
                HibernateEventListener.removeListener( TranslatedString.class, stringListener );
            }
        } );
    }
}