/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.constants;

import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.links.RawLinkable;
import edu.colorado.phet.website.util.links.RawLinker;

public class Linkers {

    /*---------------------------------------------------------------------------*
    * JARs
    *----------------------------------------------------------------------------*/

    public static final RawLinkable PHET_TRANSLATION_UTILITY_JAR = new RawLinker( "/files/translation-utility/translation-utility.jar" );
    public static final RawLinkable JAVA_COMMON_STRINGS_JAR = new RawLinker( "/sims/java-common-strings/java-common-strings_en.jar" );
    public static final RawLinkable FLASH_COMMON_STRINGS_JAR = new RawLinker( "/sims/flash-common-strings/flash-common-strings_en.jar" );


    /*---------------------------------------------------------------------------*
    * publications
    *----------------------------------------------------------------------------*/

    public static final RawLinkable PHET_DESIGN_PROCESS_PDF = new RawLinker( "/publications/phet_design_process.pdf" );
    public static final RawLinkable PHET_LOOK_AND_FEEL_PDF = new RawLinker( "/publications/PhET%20Look%20and%20Feel.pdf" );
    public static final RawLinkable CONTRIBUTION_GUIDELINES_PDF = new RawLinker( "/files/guides/Activities/PhetGuideActivityDoc_v8-final.pdf" );

    /*---------------------------------------------------------------------------*
    * social
    *----------------------------------------------------------------------------*/

    // TODO: refactor front page to use these linkers
    public static final RawLinkable FACEBOOK_PAGE = new RawLinker( "http://www.facebook.com/pages/PhET-Interactive-Simulations/161503243888932?v=wall" );
    public static final RawLinkable TWITTER_PAGE = new RawLinker( "http://twitter.com/PhETSims" );
    public static final RawLinkable BLOG = new RawLinker( "/blog/" );

    /*---------------------------------------------------------------------------*
    * installers
    *----------------------------------------------------------------------------*/


    public static final RawLinkable WINDOWS_INSTALLER = new RawLinker( "/installer/PhET-Installer_windows.exe" );
    public static final RawLinkable MAC_INSTALLER = new RawLinker( "/installer/PhET-Installer_osx.zip" );
    public static final RawLinkable LINUX_INSTALLER = new RawLinker( "/installer/PhET-Installer_linux.bin" );
    public static final RawLinkable CD_INSTALLER = new RawLinker( "/installer/PhET-Installer_cdrom.zip" );
    public static final RawLinkable DVD_INSTALLER = new RawLinker( "/installer/PhET-Installer_dvdrom.zip" );


    /*---------------------------------------------------------------------------*
    * mail
    *----------------------------------------------------------------------------*/

    public static final class HelpMailer extends RawLinker {
        public HelpMailer() {
            super( "mailto:" + WebsiteConstants.HELP_EMAIL );
        }

        public HelpMailer( String subject ) {
            super( "mailto:" + WebsiteConstants.HELP_EMAIL + "?subject=" + subject.replace( " ", "%20" ) );
        }
    }

    public static final String PHET_HELP_LINK = "<a href=\"mailto:phethelp@colorado.edu\">phethelp@colorado.edu</a>";

    public static String getHelpLink( String subject, PageContext context, PhetRequestCycle cycle ) {
        return "<a " + new HelpMailer( subject ).getHref( context, cycle ) + ">" + WebsiteConstants.HELP_EMAIL + "</a>";
    }

    /*---------------------------------------------------------------------------*
    * licensing and software agreements
    *----------------------------------------------------------------------------*/

    public static final RawLinkable SOFTWARE_AGREEMENT = new RawLinker( "/about/software-agreement_v7.htm" );
}
