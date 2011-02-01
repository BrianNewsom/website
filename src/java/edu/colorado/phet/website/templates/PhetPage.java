/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.templates;

import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.authentication.*;
import edu.colorado.phet.website.components.*;
import edu.colorado.phet.website.constants.Images;
import edu.colorado.phet.website.content.IndexPage;
import edu.colorado.phet.website.content.getphet.FullInstallPanel;
import edu.colorado.phet.website.data.Translation;
import edu.colorado.phet.website.menu.NavMenu;
import edu.colorado.phet.website.panels.LogInOutPanel;
import edu.colorado.phet.website.panels.SearchPanel;
import edu.colorado.phet.website.translation.PhetLocalizer;
import edu.colorado.phet.website.translation.TranslationUrlStrategy;
import edu.colorado.phet.website.util.HtmlUtils;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.hibernate.HibernateTask;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

/**
 * This is a page that generally has the PhET header (logo, search, sign off, etc), but can be instantiated without
 * any extras with PhetPage( params, false ). Base class for all PhET web pages.
 * <p/>
 * For now, all direct subclasses should call addTitle exactly once
 */
public abstract class PhetPage extends WebPage implements Stylable {

    /*
    Google Analytics changes: based on http://code.google.com/apis/analytics/docs/tracking/asyncUsageGuide.html
    We are now using the asynchronous method for loading GA. Under "One Push, Multiple Commands", it shows the example
    for hitting multiple trackers, which we also need to do. Also using setDomainName as before
     */

    private Locale myLocale;
    private String prefix;
    private String path;
    private String variation;

    private String title = null; // initialize as null
    private RawLabel titleLabel;

    private StringBuilder debugText = new StringBuilder();

    private String metaDescription;
    private Component metaDescriptionLabel;

    private Long initStart;

    private static final Logger logger = Logger.getLogger( PhetPage.class.getName() );

    public PhetPage( PageParameters parameters ) {
        this( parameters, true );
    }

    public PhetPage( PageParameters parameters, boolean addTemplateBindings ) {

        initStart = System.currentTimeMillis();

        if ( parameters.get( TranslationUrlStrategy.LOCALE ) != null ) {
            myLocale = (Locale) parameters.get( TranslationUrlStrategy.LOCALE );
        }
        else {
            // try again with localeString, but use english as default
            myLocale = LocaleUtils.stringToLocale( parameters.getString( TranslationUrlStrategy.LOCALE_STRING, "en" ) );
        }

        if ( parameters.getString( TranslationUrlStrategy.PREFIX_STRING ) != null ) {
            prefix = parameters.getString( TranslationUrlStrategy.PREFIX_STRING );
        }
        else {
            prefix = "/" + LocaleUtils.localeToString( myLocale ) + "/";
        }

        if ( prefix.equals( "/error/" ) ) {
            prefix = "/";
        }

        path = parameters.getString( TranslationUrlStrategy.PATH );

        // should usually default to null
        variation = parameters.getString( TranslationUrlStrategy.VARIATION );

        Session wicketSession = getSession();
        wicketSession.setLocale( myLocale );

        if ( PhetWicketApplication.get().isDevelopment() ) {
            addDebugLine( "class: " + this.getClass().getSimpleName() );
            addDebugLine( "locale: " + LocaleUtils.localeToString( myLocale ) );
            addDebugLine( "path: " + path );
            addDebugLine( "prefix: " + prefix );
            addDebugLine( "session id: " + wicketSession.getId() );

            /*
            public static final String FULL_PATH = "fullPath";
            public static final String PATH = "path";
            public static final String LOCALE_STRING = "localeString";
            public static final String PREFIX_STRING = "prefixString";
            public static final String LOCALE = "locale";
            public static final String VARIATION = "variation";
             */
            addDebugLine( "FULL_PATH: " + parameters.getString( TranslationUrlStrategy.FULL_PATH ) );
            addDebugLine( "PATH: " + parameters.getString( TranslationUrlStrategy.PATH ) );
            addDebugLine( "LOCALE_STRING: " + parameters.getString( TranslationUrlStrategy.LOCALE_STRING ) );
            addDebugLine( "PREFIX_STRING: " + parameters.getString( TranslationUrlStrategy.PREFIX_STRING ) );
            addDebugLine( "LOCALE: " + parameters.getString( TranslationUrlStrategy.LOCALE ) );
            addDebugLine( "VARIATION: " + parameters.getString( TranslationUrlStrategy.VARIATION ) );

            if ( !parameters.keySet().isEmpty() ) {
                StringBuilder builder = new StringBuilder( "<table>" );
                for ( Object o : parameters.keySet() ) {
                    builder.append( "<tr><td>" + o.toString() + "</td><td>" + parameters.get( o ).toString() + "</td></tr>" );
                }
                builder.append( "</table>" );
                addDebugLine( builder.toString() );
            }
        }

        // visual display
        if ( addTemplateBindings ) {
            // TODO: refactor static images to a single location, so paths / names can be quickly changed
            Link link = IndexPage.getLinker().getLink( "page-header-home-link", getPageContext(), getPhetCycle() );
            if ( DistributionHandler.redirectHeaderToProduction( (PhetRequestCycle) getRequestCycle() ) ) {
                link = new RawLink( "page-header-home-link", "http://phet.colorado.edu" );
            }
            add( link );
            // TODO: localize alt attributes
            link.add( new StaticImage( "page-header-logo-image", Images.PHET_LOGO, null ) );
            add( new StaticImage( "page-header-title-image", Images.LOGO_TITLE, null ) );

            //add( HeaderContributor.forCss( CSS.PHET_PAGE ) );

            switch( DistributionHandler.getSearchBoxVisibility( getPhetCycle() ) ) {
                case NONE:
                    add( new InvisibleComponent( "search-panel" ) );
                    break;
                case OFFLINE_INSTALLER:
                    add( new LocalizedText( "search-panel", "installer.mostUpToDate", new Object[]{
                            new Date(),
                            FullInstallPanel.getLinker().getHref( getPageContext(), getPhetCycle() )
                    } ) );
                    break;
                case NORMAL:
                    add( new SearchPanel( "search-panel", getPageContext() ) );
                    break;
            }

            if ( prefix.startsWith( "/translation" ) && getVariation() != null ) {
                final Translation translation = new Translation();

                HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
                    public boolean run( org.hibernate.Session session ) {
                        session.load( translation, Integer.valueOf( getVariation() ) );
                        return true;
                    }
                } );

                add( new Label( "translation-preview-notification", "This is a preview for translation #" + getVariation() +
                                                                    " of " + translation.getLocale().getDisplayName() +
                                                                    " (" + LocaleUtils.localeToString( translation.getLocale() ) +
                                                                    ")" ) );
            }
            else {
                add( new InvisibleComponent( "translation-preview-notification" ) );
            }

            boolean isAdmin = PhetSession.get().isSignedIn() && PhetSession.get().getUser().isTeamMember();

            if ( !isAdmin && ( this instanceof SignInPage || this instanceof RegisterPage || this instanceof EditProfilePage ) ) {
                add( new InvisibleComponent( "log-in-out-panel" ) );
            }
            else {
                add( new LogInOutPanel( "log-in-out-panel", getPageContext() ) );
            }
        }

        logger.debug( "request cycle is a : " + getRequestCycle().getClass().getSimpleName() );

        add( new RawBodyLabel( "debug-page-class", "<!-- class " + getClass().getCanonicalName() + " -->" ) );
        if ( getPhetCycle().isForProductionServer() ) {
            add( new Label( "autotracking" ) ); // enable autotracking script
        }
        else {
            add( new InvisibleComponent( "autotracking" ) ); // disable autotracking
        }
        add( new RawBodyLabel( "debug-page-host", "<!-- host " + getPhetCycle().getWebRequest().getHttpServletRequest().getServerName() + " -->" ) );
    }

    public Locale getMyLocale() {
        return myLocale;
    }

    /**
     * @return The prefix of the URL, which generally represents /XX/ where XX is the locale.
     */
    public String getMyPrefix() {
        return prefix;
    }

    /**
     * @return The relative path underneath the prefix. Thus for a URL of http://phet.colorado.edu/en/simulations/new,
     *         this would return 'simulations/new' (the prefix would be /en/)
     */
    public String getMyPath() {
        if ( path == null ) {
            return "";
        }
        return path;
    }

    /**
     * @return Everything past 'http://phet.colorado.edu' in the URI.
     */
    public String getFullPath() {
        String p = prefix + path;
        String queryString = getPhetCycle().getQueryString();
        if ( queryString != null ) {
            p += "?" + queryString;
        }
        return p;
    }

    public PageContext getPageContext() {
        return new PageContext( getMyPrefix(), getMyPath(), getMyLocale() );
    }

    @Override
    public Locale getLocale() {
        return myLocale;
    }

    /**
     * @param title The string passed in should be properly escaped!
     */
    public void setTitle( String title ) {
        if ( hasTitle() ) {
            remove( titleLabel );
        }
        this.title = title;
        titleLabel = new RawLabel( "page-title", title );
        add( titleLabel );
    }

    public Boolean hasTitle() {
        return this.title != null && titleLabel != null;
    }

    public String getTitle() {
        return title;
    }

    public org.hibernate.Session getHibernateSession() {
        return ( (PhetRequestCycle) getRequestCycle() ).getHibernateSession();
    }

    public NavMenu getNavMenu() {
        return ( (PhetWicketApplication) getApplication() ).getMenu();
    }

    private Long renderStart;

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        renderStart = System.currentTimeMillis();
        logger.debug( "Pre-render: " + ( renderStart - initStart ) + " ms" );
        //logger.debug( "stack trace: ", new Exception() );
        logger.debug( "Debug: page stateless = " + isPageStateless() );
        addDebugLine( "pre-render: " + ( renderStart - initStart ) + " ms" );
        addDebugLine( "page stateless: <span style='color: #" + ( isPageStateless() ? "00ff00" : "ff0000" ) + "'>" + isPageStateless() + "</span>" );

        // add a debug panel in that shows us some info
        if ( PhetWicketApplication.get().isDevelopment() ) {
            add( new AbstractHeaderContributor() {
                @Override
                public IHeaderContributor[] getHeaderContributors() {
                    return new IHeaderContributor[]{
                            new IHeaderContributor() {
                                public void renderHead( IHeaderResponse response ) {
                                    response.renderString( "<script type=\"text/javascript\">addEventListener( 'load', function() {\n" +
                                                           "            var div = document.createElement( 'div' );\n" +
                                                           "            div.style.position = \"fixed\";\n" +
                                                           "            div.style.right = \"0\";\n" +
                                                           "            div.style.bottom = \"0\";\n" +
                                                           "            div.style.border = \"1px solid #888\";\n" +
                                                           "            div.style.padding = \"0.3em\";\n" +
                                                           "            div.style.background = \"#fff\";\n" +
                                                           "            div.style.maxWidth = \"300px\";\n" +
                                                           "            div.id = \"phet-page-debug\";\n" +
                                                           "            div.innerHTML = \"" + debugText.toString() + "\";\n" +
                                                           "            document.body.appendChild( div );\n" +
                                                           "        }, false );</script>" );
                                }
                            }
                    };
                }
            } );
        }

        // add meta description if it does not already exist
        if ( metaDescriptionLabel == null ) {
            if ( metaDescription == null || !getLocale().equals( PhetWicketApplication.getDefaultLocale() ) ) {
                metaDescriptionLabel = new InvisibleComponent( "metaDescription" );
            }
            else {
                metaDescriptionLabel = new RawLabel( "metaDescription", "<meta name=\"description\" content=\"" + HtmlUtils.encodeForAttribute( metaDescription ) + "\"/>" ) {{
                    setRenderBodyOnly( true );
                }};
            }
            add( metaDescriptionLabel );
        }
    }

    public void setMetaDescription( String desc ) {
        metaDescription = desc;
    }

    public void setMetaDescriptionKey( String key ) {
        metaDescription = getPhetLocalizer().getString( key, this );
    }

    public void addDebugLine( String str ) {
        if ( PhetWicketApplication.get().isDevelopment() ) {
            debugText.append( str ).append( "<br/>" );
        }
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
        logger.debug( "Render: " + ( System.currentTimeMillis() - renderStart ) + " ms" );
    }

    @Override
    protected void onDetach() {
        logger.debug( "Detaching page" );
        super.onDetach();
    }

    @Override
    public String getVariation() {
        return variation;
    }

    public PhetRequestCycle getPhetCycle() {
        return (PhetRequestCycle) getRequestCycle();
    }

    public PhetLocalizer getPhetLocalizer() {
        return (PhetLocalizer) getLocalizer();
    }

    public ServletContext getServletContext() {
        return ( (PhetWicketApplication) getApplication() ).getServletContext();
    }

    public String getStyle( String key ) {
        if ( key.equals( "style.body.id" ) ) {
            if ( getPhetCycle().isOfflineInstaller() ) {
                return "offline-installer-body";
            }
            else {
                return "other-body";
            }
        }
        return "";
    }

    /**
     * If the user is not signed in, redirect them to the sign-in page.
     */
    protected void verifySignedIn() {
        AuthenticatedPage.checkSignedIn( getPageContext() );
    }

    /**
     * @param component The component to add
     * @param id        The HTML id attribute
     * @return The page itself
     */
    public PhetPage addWithId( Component component, String id ) {
        add( component );
        component.setMarkupId( id );
        component.setOutputMarkupId( true );
        return this; // similar to MarkupContainer.add()
    }

}