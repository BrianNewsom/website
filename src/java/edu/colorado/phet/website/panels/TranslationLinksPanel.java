/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.panels;

import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.ResourceModel;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.common.phetcommon.util.function.Function1;
import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.cache.EventDependency;
import edu.colorado.phet.website.components.LocalizedLabel;
import edu.colorado.phet.website.components.RawLink;
import edu.colorado.phet.website.data.TranslatedString;
import edu.colorado.phet.website.data.util.HibernateEventListener;
import edu.colorado.phet.website.data.util.IChangeListener;
import edu.colorado.phet.website.util.attributes.ClassAppender;
import edu.colorado.phet.website.util.HtmlUtils;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

import static edu.colorado.phet.common.phetcommon.util.FunctionalUtils.filter;
import static edu.colorado.phet.common.phetcommon.util.FunctionalUtils.map;

/**
 * Shows a list of the available website translations with the currently viewed one grayed. Clicking the links to
 * other translations will take one to the exact same page, but for the other translation.
 */
public class TranslationLinksPanel extends PhetPanel {

    private static final Logger logger = Logger.getLogger( TranslationLinksPanel.class.getName() );

    public TranslationLinksPanel( String id, final PageContext context ) {
        super( id, context );

        final String queryString = HtmlUtils.encodeForAttribute( getPhetCycle().getQueryString() == null ? "" : "?" + getPhetCycle().getQueryString() );

        Locale englishLocale = LocaleUtils.stringToLocale( "en" );
        PageContext englishContext = context.withNewLocale( englishLocale );
        String linkTo = englishContext.getPrefix() + HtmlUtils.encode( englishContext.getPath() );
        if ( linkTo.equals( "/en/" ) ) {
            linkTo = "/";
        }
        if ( DistributionHandler.redirectEnglishLinkToPhetMain( (PhetRequestCycle) getRequestCycle() ) ) {
            linkTo = "http://phet.colorado.edu";
        }
        linkTo += queryString;
        Link englishLink = new RawLink( "translation-link", linkTo );
        LocalizedLabel englishLabel = new LocalizedLabel( "translation-label", englishLocale, new ResourceModel( "language.name" ) );
        englishLink.add( englishLabel );
        if ( context.getLocale().equals( englishLocale ) ) {
            englishLabel.add( new ClassAppender( "current-locale" ) );
        }
        add( englishLink );

        List<String> translationLocaleStrings = PhetWicketApplication.get().getTranslationLocaleStrings();

        if ( getPhetCycle().isOfflineInstaller() ) {
            // if we are being ripped for an offline installer, ONLY show the locales that are requested

            translationLocaleStrings = filter( // filter out English, since this is already shown
                    map( DistributionHandler.shownLocales( PhetRequestCycle.get() ),
                         new Function1<Locale, String>() {
                             public String apply( Locale locale ) {
                                 return LocaleUtils.localeToString( locale );
                             }
                         } ), new Function1<String, Boolean>() {
                public Boolean apply( String localeString ) {
                    return !localeString.equals( "en" );
                }
            } );
        }

        ListView listView = new ListView<String>( "translation-links", translationLocaleStrings ) {
            protected void populateItem( ListItem<String> item ) {
                String localeString = item.getModelObject();
                Locale locale = LocaleUtils.stringToLocale( localeString );
                PageContext newContext = context.withNewLocale( locale );
                String path = newContext.getPrefix() + HtmlUtils.encode( newContext.getPath() ) + queryString;
                Link link = new RawLink( "translation-link", path );
                LocalizedLabel label = new LocalizedLabel( "translation-label", locale, new ResourceModel( "language.name" ) );
                link.add( label );
                if ( context.getLocale().equals( locale ) ) {
                    label.add( new ClassAppender( "current-locale" ) );
                }
                item.add( link );
            }
        };
        add( listView );

        addDependency( new EventDependency() {
            private IChangeListener stringListener;

            @Override
            protected void addListeners() {
                stringListener = createTranslationChangeInvalidator( context.getLocale() );
                HibernateEventListener.addListener( TranslatedString.class, stringListener );
                PhetWicketApplication.get().addTranslationChangeListener( getTranslationChangeInvalidator() );
            }

            @Override
            protected void removeListeners() {
                HibernateEventListener.removeListener( TranslatedString.class, stringListener );
                PhetWicketApplication.get().removeTranslationChangeListener( getTranslationChangeInvalidator() );
            }
        } );

        //add( HeaderContributor.forCss( CSS.TRANSLATION_LINKS ) );
    }
}
