/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.forteachers;

import org.apache.wicket.Component;

import edu.colorado.phet.website.content.workshops.WorkshopsPanel;
import edu.colorado.phet.website.templates.PhetMenuPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.RawLinkable;
import edu.colorado.phet.website.util.wicket.IComponentFactory;
import edu.colorado.phet.website.util.wicket.WicketUtils;

public class VirtualWorkshopPanel extends ForTeachersPanel {

    public VirtualWorkshopPanel( String id, PageContext context ) {
        super( id, context );

        // add linkers
        add( WorkshopsPanel.getLinker().getLink( "workshops-link", context, getPhetCycle() ) );
    }

    public static String getKey() {
        return "forTeachers.virtualWorkshop";
    }

    public static String getUrl() {
        return "teaching-resources/virtualWorkshop";
    }

    public static RawLinkable getLinker() {
        return new AbstractLinker() {
            public String getSubUrl( PageContext context ) {
                return getUrl();
            }
        };
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        addRighthandMenu( getKey() );
    }
}