/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.simulations;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;

import edu.colorado.phet.website.data.Category;
import edu.colorado.phet.website.menu.NavLocation;
import edu.colorado.phet.website.templates.PhetRegularPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.RawLinkable;

public class ByGradeLevelPage extends PhetRegularPage {

    private static final Logger logger = Logger.getLogger( ByGradeLevelPage.class.getName() );

    public ByGradeLevelPage( final PageParameters parameters ) {
        super( parameters );

        add( new ByGradeLevelPanel( "by-grade-level-panel", getPageContext() ) );

        NavLocation location = getNavMenu().getLocationByKey( Category.BY_LEVEL_CATEGORY_NAME );

        initializeLocation( location );
        setTitle( StringUtils.messageFormat( getLocalizer().getString( "simulationDisplay.title", this ), new Object[] {
                getPhetLocalizer().getString( location.getLocalizationKey(), this )
        } ) );

        this.getPhetCycle().setMinutesToCache( 15 );
    }

    public static void addToMapper( PhetUrlMapper mapper ) {
        // WARNING: don't change without also changing the old URL redirection
        mapper.addMap( "^simulations/category/by-level$", ByGradeLevelPage.class );
    }

    public static RawLinkable getLinker() {
        // WARNING: don't change without also changing the old URL redirection
        return new AbstractLinker() {
            public String getSubUrl( PageContext context ) {
                return "simulations/category/by-level";
            }
        };
    }

}