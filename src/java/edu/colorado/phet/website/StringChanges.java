/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import edu.colorado.phet.common.phetcommon.view.util.StringUtil;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

/**
 * Contains strings that have been addedor modified since the last production deployment. If strings by those key names
 * don't exist they will be created.
 */
public class StringChanges {

    private static Logger logger = Logger.getLogger( StringChanges.class );

    public static void checkNewStrings() {
        Session session = HibernateUtils.getInstance().openSession();

        StringUtils.addString( session, "research.publications.other", "Other Work by PhET Researchers" );
        StringUtils.addString( session, "nav.html.licensing", "HTML5 Licensing" );
        StringUtils.addString( session, "html.licensing.title", "HTML5 Simulation Licensing Information" );
        StringUtils.addString( session, "html.licensing.comingSoon", "Information coming soon, please contact {0}." );

        // add strings for new trouble shooting pages
        StringUtils.addString( session, "nav.troubleshooting.mac", "Mac" );
        StringUtils.addString( session, "troubleshooting.mac.title", "Troubleshooting Mac" );
        StringUtils.addString( session, "nav.troubleshooting.windows", "Windows" );
        StringUtils.addString( session, "troubleshooting.windows.title", "Troubleshooting Windows" );
        StringUtils.addString( session, "nav.troubleshooting.mobile", "Tablets/Mobile Devices" );
        StringUtils.addString( session, "troubleshooting.mobile.title", "Troubleshooting Tablets/Mobile Devices" );

        // update some troubleshooting strings
        StringUtils.deleteString( session, "troubleshooting.main.q1.answer" );
        StringUtils.addString( session, "troubleshooting.main.q1.answer", "<p>Some of our simulations are Java Web Start based applications and others use Macromedia's Flash player. Flash comes with most computers while Java Web Start is a free application that can be downloaded from Sun Microsystems. To run the Java-based simulations you must have Java version 1.5 or higher installed on your computer.</p><p>To download the latest version of Java, please visit: <a {0}>https://www.java.com</a>.</p>" );

        StringUtils.deleteString( session, "about.p4" );
        StringUtils.addString( session, "about.p4", "All PhET simulations are freely available from the <a {0}>PhET website</a> and are easy to use and incorporate into the classroom. They are written in Java and Flash, and can be run using a standard web browser as long as Flash and Java are installed." );

        session.close();
    }

    /*---------------------------------------------------------------------------*
    * deprecated strings:
    * newsletter-instructions
    * about.who-we-are.title
    *----------------------------------------------------------------------------*/

}
