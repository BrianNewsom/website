/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.link.Link;

import edu.colorado.phet.website.cache.SimplePanelCacheEntry;
import edu.colorado.phet.website.components.RawLink;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.panels.simulation.SimulationMainPanel;
import edu.colorado.phet.website.templates.PhetPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.links.RawLinkable;

public class IndexPage extends PhetPage {
    public IndexPage( PageParameters parameters ) {
        super( parameters );

        setTitle( getLocalizer().getString( "home.title", this ) );

        add( new SimplePanelCacheEntry( SimulationMainPanel.class, null, getPageContext().getLocale(), getMyPath(), getPhetCycle() ) {
            public PhetPanel constructPanel( String id, PageContext context ) {
                return new IndexPanel( id, context );
            }
        }.instantiate( "index-panel", getPageContext(), getPhetCycle() ) );
        //add( new IndexPanel( "index-panel", getPageContext() ) );

        // TODO: localize
        setMetaDescriptionKey( "home.meta" );

        this.getPhetCycle().setMinutesToCache( 60 );
    }

    public static void addToMapper( PhetUrlMapper mapper ) {
        mapper.addMap( "^$", IndexPage.class );
    }

    public static RawLink createLink( String id, PageContext context ) {
        if ( context.getPrefix().equals( "/en/" ) ) {
            return new RawLink( id, "/" );
        }
        else {
            return new RawLink( id, context.getPrefix() );
        }
    }

    public static RawLinkable getLinker() {
        return new RawLinkable() {
            public String getRawUrl( PageContext context, PhetRequestCycle cycle ) {
                if ( context.getPrefix().equals( "/en/" ) ) {
                    return "/";
                }
                else {
                    return context.getPrefix();
                }
            }

            public String getHref( PageContext context, PhetRequestCycle cycle ) {
                return "href=\"" + getRawUrl( context, cycle ) + "\"";
            }

            public String getDefaultRawUrl() {
                return "/";
            }

            public Link getLink( String id, PageContext context, PhetRequestCycle cycle ) {
                return new RawLink( id, getRawUrl( context, cycle ) );
            }
        };
    }
}