/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content.troubleshooting;

import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.components.LocalizedText;
import edu.colorado.phet.website.constants.Linkers;
import edu.colorado.phet.website.content.getphet.FullInstallPanel;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.links.AbstractLinker;
import edu.colorado.phet.website.util.links.RawLinkable;


/**
 * General troubleshooting panel
 */
public class TroubleshootingMainPanel extends PhetPanel {
    public TroubleshootingMainPanel( String id, PageContext context ) {
        super( id, context );

        add( TroubleshootingMacPanel.getLinker().getLink( "to-mac", context, getPhetCycle() ) );
        add( TroubleshootingWindowsPanel.getLinker().getLink( "to-windows", context, getPhetCycle() ) );
        add( TroubleshootingMobilePanel.getLinker().getLink( "to-mobile", context, getPhetCycle() ) );

        add( new LocalizedText( "troubleshooting-main-intro", "troubleshooting.main.intro", new Object[]{
                Linkers.getHelpLink( "Help", context, getPhetCycle() ),
                ""
        } ) );

        add( new LocalizedText( "troubleshooting-main-q1-answer", "troubleshooting.main.q1.answer", new Object[]{
                "<a href=\"https://www.java.com/\">https://www.java.com</a>",
                GeneralFAQPanel.getLinker().getHrefWithHash( context, getPhetCycle(), "q2-header" )
        } ) );


        add( new LocalizedText( "troubleshooting-main-q5-answer", "troubleshooting.main.q5.answer", new Object[]{
                Linkers.getHelpLink( "Flash Simulations", context, getPhetCycle() )
        } ) );

        add( new LocalizedText( "troubleshooting-main-q8-answer", "troubleshooting.main.q8.answer", new Object[]{
                Linkers.getHelpLink( "Laptop Performance Issues", context, getPhetCycle() )
        } ) );

        add( new LocalizedText( "troubleshooting-main-q9-answer", "troubleshooting.main.q9.answer", new Object[]{
                Linkers.getHelpLink( "Sound Issues", context, getPhetCycle() )
        } ) );

        add( new LocalizedText( "troubleshooting-main-q11-answer", "troubleshooting.main.q11.answer", new Object[]{
                FullInstallPanel.getLinker().getHref( context, getPhetCycle() )
        } ) );

        add( new LocalizedText( "troubleshooting-main-q12-answer", "troubleshooting.main.q12.answer" ) );

        add( new LocalizedText( "troubleshooting-main-q18-answer", "troubleshooting.main.q18.answer" ) );

    }

    public static String getKey() {
        return "troubleshooting.main";
    }

    public static String getUrl() {
        return "troubleshooting";
    }

    public static RawLinkable getLinker() {
        return new AbstractLinker() {
            @Override
            public String getRawUrl( PageContext context, PhetRequestCycle cycle ) {
                if ( DistributionHandler.redirectPageClassToProduction( cycle, TroubleshootingMainPanel.class ) ) {
                    return "http://phet.colorado.edu/tech_support/index.php";
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