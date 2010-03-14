package edu.colorado.phet.website.panels;

import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.html.panel.Panel;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.menu.NavMenu;
import edu.colorado.phet.website.translation.PhetLocalizer;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

/**
 * Standard panel that almost all panels in the website should inherit from. Allows it to be translated, pulls the
 * correct locale out of the page context automatically, has enough convenience functions, and organizes stylesheets.
 */
public class PhetPanel extends Panel {

    private Locale myLocale;

    public PhetPanel( String id, PageContext context ) {
        super( id );
        this.myLocale = context.getLocale();

        addStylesheets();
    }

    /**
     * Considered immutable
     *
     * @return Direct access to the panel's locale
     */
    public Locale getMyLocale() {
        return myLocale;
    }

    /**
     * @return The currently active Hibernate session. The session is bound at the start of the request, and released
     *         after the response, and can be used to start transactions. @see HibernateUtils#wrapTransaction(HibernateTask)
     */
    public org.hibernate.Session getHibernateSession() {
        return ( (PhetRequestCycle) getRequestCycle() ).getHibernateSession();
    }

    /**
     * @return The navigation menu data structure for the website
     */
    public NavMenu getNavMenu() {
        return ( (PhetWicketApplication) getApplication() ).getMenu();
    }

    /**
     * Override locale, so that localized strings with wicket:message will use this panel's locale
     * <p/>
     * Considered immutable, do not modify
     */
    @Override
    public Locale getLocale() {
        return myLocale;
    }

    /**
     * @return The request cycle associated with the request and response. Contains information about the request, and
     *         can be used to control the behavior of the response (and redirections)
     */
    public PhetRequestCycle getPhetCycle() {
        return (PhetRequestCycle) getRequestCycle();
    }

    /**
     * @return The PhET localizer, which can be used to directly receive translated strings. It caches them, so this is
     *         much better than (and easier than) searching the database
     */
    public PhetLocalizer getPhetLocalizer() {
        return (PhetLocalizer) getLocalizer();
    }

    /**
     * @return The Java J2EE servlet context associated with this response / thread.
     */
    public ServletContext getServletContext() {
        return ( (PhetWicketApplication) getApplication() ).getServletContext();
    }

    /**
     * Override this and add the stylesheets required. This is desired so that if the component is cached, the correct
     * stylesheets can be added back into the header.
     */
    public void addStylesheets() {

    }
}
