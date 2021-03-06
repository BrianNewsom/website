/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.simulations;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.protocol.http.WebResponse;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.colorado.phet.website.cache.SimplePanelCacheEntry;
import edu.colorado.phet.website.constants.WebsiteConstants;
import edu.colorado.phet.website.content.NotFoundPage;
import edu.colorado.phet.website.data.Category;
import edu.colorado.phet.website.data.LocalizedSimulation;
import edu.colorado.phet.website.data.Simulation;
import edu.colorado.phet.website.menu.NavLocation;
import edu.colorado.phet.website.metadata.SimulationPageMetadataPanel;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.panels.simulation.SimulationMainPanel;
import edu.colorado.phet.website.templates.PhetMenuPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;
import edu.colorado.phet.website.util.links.AbstractLinker;

/**
 * Main page for simulations
 */
public class SimulationPage extends PhetMenuPage {

    private static final Logger logger = Logger.getLogger( SimulationPage.class.getName() );

    public SimulationPage( PageParameters parameters ) {
        super( parameters );

        String flavorName = parameters.getString( "simulation" );

        LocalizedSimulation simulation = null;
        Set<NavLocation> locations = new HashSet<NavLocation>();

        Session session = getHibernateSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            simulation = HibernateUtils.getBestSimulation( session, getMyLocale(), flavorName );
            if ( simulation != null ) {
                for ( Object o : simulation.getSimulation().getCategories() ) {
                    Category category = (Category) o;
                    locations.add( category.getNavLocation( getNavMenu() ) );
                }
            }
            tx.commit();
        }
        catch( RuntimeException e ) {
            logger.warn( e );
            if ( tx != null && tx.isActive() ) {
                try {
                    tx.rollback();
                }
                catch( HibernateException e1 ) {
                    logger.error( "ERROR: Error rolling back transaction", e1 );
                }
                throw e;
            }
        }

        if ( simulation == null || !simulation.getSimulation().isVisible() ) {
            throw new RestartResponseAtInterceptPageException( NotFoundPage.class );
        }

        setMetaDescription( getLocalizer().getString( simulation.getSimulation().getDescriptionKey(), this ) );

        add( new SimulationPageMetadataPanel( "metadata-tags", getPageContext(), simulation ) );

        final LocalizedSimulation finalSim = simulation;
        PhetPanel simPanel = new SimplePanelCacheEntry( SimulationMainPanel.class, null, getPageContext().getLocale(), getMyPath(), getPhetCycle() ) {
            public PhetPanel constructPanel( String id, PageContext context ) {
                return new SimulationMainPanel( id, finalSim, context );
            }
        }.instantiate( "simulation-main-panel", getPageContext(), getPhetCycle() );

        //SimulationMainPanel simPanel = new SimulationMainPanel( "simulation-main-panel", simulation, getPageContext() );
        add( simPanel );
        //addTitle( simPanel.getTitle() );
        setTitle( (String) simPanel.getCacheParameter( "title" ) );

        initializeLocationWithSet( locations );

        this.getPhetCycle().setMinutesToCache( 15 );

    }

    public static void addToMapper( PhetUrlMapper mapper ) {
        mapper.addMap( "^simulation/([^/]+)$", SimulationPage.class, new String[]{"simulation"} );
    }

    public static AbstractLinker getLinker( final String projectName, final String simulationName ) {
        // WARNING: don't change without also changing the old URL redirection
        return new AbstractLinker() {
            @Override
            public String getSubUrl( PageContext context ) {
                return "simulation/" + simulationName;
            }
        };
    }

    public static AbstractLinker getLinker( final Simulation simulation ) {
        return getLinker( simulation.getProject().getName(), simulation.getName() );
    }

    public static AbstractLinker getLinker( final LocalizedSimulation localizedSimulation ) {
        return getLinker( localizedSimulation.getSimulation() );
    }
}