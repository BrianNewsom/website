/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.forteachers;

import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.RawLinkable;

public class LectureOverviewPanel extends PhetPanel {
    public LectureOverviewPanel( String id, PageContext context ) {
        super( id, context );

    }

    public static String getKey() {
        return "usingPhetInLecture";
    }

    public static String getUrl() {
        return "for-teachers/usingPhetInLecture";
    }

    public static RawLinkable getLinker() {
        return new AbstractLinker() {
            @Override
            public String getRawUrl( PageContext context, PhetRequestCycle cycle ) {
                if ( cycle != null && DistributionHandler.redirectPageClassToProduction( cycle, LectureOverviewPanel.class ) ) {
                    return "http://phet.colorado.edu/teacher_ideas/usingPhetInLecture.php";
                }
                else {
                    return super.getRawUrl( context, cycle );
                }
            }

            public String getSubUrl( PageContext context ) {
                return getUrl();
            }
        };
    }
}