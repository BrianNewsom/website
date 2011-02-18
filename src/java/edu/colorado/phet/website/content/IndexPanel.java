/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.content;

import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;

import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.components.InvisibleComponent;
import edu.colorado.phet.website.components.LocalizedText;
import edu.colorado.phet.website.components.RawLink;
import edu.colorado.phet.website.components.StaticImage;
import edu.colorado.phet.website.constants.Images;
import edu.colorado.phet.website.content.about.*;
import edu.colorado.phet.website.content.contribution.ContributionBrowsePage;
import edu.colorado.phet.website.content.getphet.FullInstallPanel;
import edu.colorado.phet.website.content.getphet.OneAtATimePanel;
import edu.colorado.phet.website.content.getphet.RunOurSimulationsPanel;
import edu.colorado.phet.website.content.simulations.CategoryPage;
import edu.colorado.phet.website.content.troubleshooting.FAQPanel;
import edu.colorado.phet.website.content.troubleshooting.TroubleshootingMainPanel;
import edu.colorado.phet.website.content.workshops.WorkshopsPanel;
import edu.colorado.phet.website.newsletter.InitialSubscribePage;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.panels.RotatorFallbackPanel;
import edu.colorado.phet.website.panels.RotatorPanel;
import edu.colorado.phet.website.panels.TranslationLinksPanel;
import edu.colorado.phet.website.panels.sponsor.FeaturedSponsorPanel;
import edu.colorado.phet.website.translation.TranslationMainPage;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

/**
 * The panel which represents the main content portion of the home (index) page
 */
public class IndexPanel extends PhetPanel {

    public static final String PLAY_SIMS_ID = "play-sims";

    public IndexPanel( String id, PageContext context ) {
        super( id, context );

        add( new StaticImage( "ksu-logo", Images.LOGO_ERCSME_SMALL, null ) );
        add( new StaticImage( "nsf-logo", Images.LOGO_NSF_SMALL, null ) );
        add( new StaticImage( "hewlett-logo", Images.LOGO_HEWLETT_SMALL, null ) );

        if ( getMyLocale().equals( new Locale( "en" ) ) ) {
            add( new Label( "odf-and", "and the" ) );
        }
        else {
            add( new InvisibleComponent( "odf-and" ) );
        }

        add( new LocalizedText( "facebook-text", "home.facebookText", new Object[]{
                "<img class=\"index-social-image\" src=\"/images/icons/social/facebook.png\" alt=\"Facebook icon\" width=\"16\" height=\"16\"/>"
        } ) );
        add( new LocalizedText( "twitter-text", "home.twitterText", new Object[]{
                "<img class=\"index-social-image\" src=\"/images/icons/social/twitter.png\" alt=\"Twitter icon\" width=\"16\" height=\"16\"/>"
        } ) );
        add( new LocalizedText( "blog-text", "home.blogText" ) );
        add( InitialSubscribePage.getLinker().getLink( "subscribe-link", context, getPhetCycle() ) );

        add( new LocalizedText( "index-main-text", "home.subheader", new Object[]{
                ResearchPanel.getLinker().getHref( context, getPhetCycle() )
        } ) );

        addWithId( CategoryPage.getDefaultLinker().getLink( "play-sims-link", context, getPhetCycle() ), PLAY_SIMS_ID );

        add( RunOurSimulationsPanel.getLinker().getLink( "run-our-sims-link", context, getPhetCycle() ) );
        add( CategoryPage.getDefaultLinker().getLink( "on-line-link", context, getPhetCycle() ) );
        add( FullInstallPanel.getLinker().getLink( "full-install-link", context, getPhetCycle() ) );
        add( OneAtATimePanel.getLinker().getLink( "one-at-a-time-link", context, getPhetCycle() ) );
        add( TroubleshootingMainPanel.getLinker().getLink( "troubleshooting-link", context, getPhetCycle() ) );
        add( FAQPanel.getLinker().getLink( "faqs-link", context, getPhetCycle() ) );

        add( WorkshopsPanel.getLinker().getLink( "workshops-link", context, getPhetCycle() ) );

        add( DonatePanel.getLinker().getLink( "support-phet-link", context, getPhetCycle() ) );
        add( TranslationUtilityPanel.getLinker().getLink( "translate-sims-link", context, getPhetCycle() ) );

        add( AboutMainPanel.getLinker().getLink( "about-general", context, getPhetCycle() ) );
        add( AboutMainPanel.getLinker().getLink( "about-phet", context, getPhetCycle() ) );
        add( AboutNewsPanel.getLinker().getLink( "about-news", context, getPhetCycle() ) );
        add( AboutContactPanel.getLinker().getLink( "about-contact", context, getPhetCycle() ) );
        add( new LocalizedText( "other-sponsors", "home.about.alongWithOurSponsors", new Object[]{
                AboutSponsorsPanel.getLinker().getHref( context, getPhetCycle() )
        } ) );
        add( AboutSponsorsPanel.getLinker().getLink( "sponsors-general", context, getPhetCycle() ) );

        add( new FeaturedSponsorPanel( "featured-sponsor-panel", context ) );

        if ( context.getLocale().equals( PhetWicketApplication.getDefaultLocale() ) && DistributionHandler.displayTranslationEditLink( (PhetRequestCycle) getRequestCycle() ) ) {
            add( TranslationMainPage.getLinker().getLink( "test-translation", context, getPhetCycle() ) );
        }
        else {
            add( new InvisibleComponent( "test-translation" ) );
        }

        if ( !getMyLocale().equals( PhetWicketApplication.getDefaultLocale() ) && getPhetLocalizer().getString( "translation.credits", this ).length() > 0 ) {
            add( new LocalizedText( "translation-credits", "translation.credits" ) );
        }
        else {
            add( new InvisibleComponent( "translation-credits" ) );
        }

        if ( DistributionHandler.displayTranslationLinksPanel( (PhetRequestCycle) getRequestCycle() ) ) {
            add( new TranslationLinksPanel( "translation-links", context ) );
        }
        else {
            add( new InvisibleComponent( "translation-links" ) );
        }

        if ( DistributionHandler.redirectActivities( (PhetRequestCycle) getRequestCycle() ) ) {
            add( new RawLink( "activities-link", "http://phet.colorado.edu/teacher_ideas/index.php" ) );
            add( new RawLink( "browse-activities-link", "http://phet.colorado.edu/teacher_ideas/browse.php" ) );
            add( new RawLink( "submit-activity-link", "http://phet.colorado.edu/teacher_ideas/index.php" ) );
        }
        else {
            add( TeacherIdeasPanel.getLinker().getLink( "activities-link", context, getPhetCycle() ) );
            if ( getPhetCycle().isOfflineInstaller() ) {
                add( new InvisibleComponent( "browse-activities-link" ) );
            }
            else {
                add( ContributionBrowsePage.getLinker().getLink( "browse-activities-link", context, getPhetCycle() ) );
            }
            add( TeacherIdeasPanel.getLinker().getLink( "submit-activity-link", context, getPhetCycle() ) );
        }

        if ( DistributionHandler.showRotatorFallback( getPhetCycle() ) ) {
            add( new RotatorFallbackPanel( "rotator-panel", context ) );
        }
        else {
            add( new RotatorPanel( "rotator-panel", context ) );
        }

        add( AboutLicensingPanel.getLinker().getLink( "some-rights-link", context, getPhetCycle() ) );
    }

}
