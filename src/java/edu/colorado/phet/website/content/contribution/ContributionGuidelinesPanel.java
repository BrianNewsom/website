/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.contribution;

import edu.colorado.phet.website.components.LocalizedText;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.RawLinkable;

public class ContributionGuidelinesPanel extends PhetPanel {
    public ContributionGuidelinesPanel( String id, PageContext context ) {
        super( id, context );

        //add( HeaderContributor.forCss( CSS.CONTRIBUTION_MAIN ) );
        add( new LocalizedText( "example-activities", "teacherIdeas.guide.similarActivities", new Object[] {
                ContributionBrowsePage.getLinker().getHref( context, getPhetCycle() )
        } ) );

    }

    public static String getKey() {
        return "teacherIdeas.guide";
    }

    public static String getUrl() {
        return "for-teachers/activity-guide";
    }

    public static RawLinkable getLinker() {
        return new AbstractLinker() {
            public String getSubUrl( PageContext context ) {
                return getUrl();
            }
        };
    }
}