/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation.entities;

import edu.colorado.phet.website.content.DonatePanel;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.translation.PhetPanelFactory;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

public class DonateEntity extends TranslationEntity {
    public DonateEntity() {
        addString( "donate.title" );
        addString( "donate.header" );
        addString( "donate.ifUseSimulations" );
        addString( "donate.anySize" );
        addString( "donate.donateNow" );
        addString( "donate.donateNowBang" );
        addString( "donate.questions" );
        addString( "donate.note" );
        addString( "donation-banner.donateToAdvanceScience" );
        addString( "donation-banner.phetsAnnualCampaign" );
        addString( "donation-banner.donateNow" );
        addString( "donation-banner.phetsEndOfSchoolCampaign" );
        addString( "donation-banner.summerVacation" );
        addString( "donation-banner.html5.tooltip" );

        addPreview( new PhetPanelFactory() {
                        public PhetPanel getNewPanel( String id, PageContext context, PhetRequestCycle requestCycle ) {
                            return new DonatePanel( id, context );
                        }
                    }, "Donate page" );
    }

    public String getDisplayName() {
        return "Donate";
    }
}