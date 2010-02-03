package edu.colorado.phet.website.panels;

import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.html.panel.Panel;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.translation.PhetLocalizer;
import edu.colorado.phet.website.menu.NavMenu;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

public class PhetPanel extends Panel {

    private Locale myLocale;

    public PhetPanel( String id, PageContext context ) {
        super( id );
        this.myLocale = context.getLocale();
    }

    public Locale getMyLocale() {
        return myLocale;
    }

    public org.hibernate.Session getHibernateSession() {
        return ( (PhetRequestCycle) getRequestCycle() ).getHibernateSession();
    }

    public NavMenu getNavMenu() {
        return ( (PhetWicketApplication) getApplication() ).getMenu();
    }

    /**
     * Override locale, so that localized strings with wicket:message will use this panel's locale
     */
    @Override
    public Locale getLocale() {
        return myLocale;
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
}
