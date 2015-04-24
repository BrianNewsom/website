/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

/**
 * Contains strings that have been added or modified since the last production deployment. If strings by those key names
 * don't exist they will be created.
 */
public class StringChanges {

    private static Logger logger = Logger.getLogger( StringChanges.class );

    public static void checkNewStrings() {
        Session session = HibernateUtils.getInstance().openSession();

//        StringUtils.deleteString( session, "simulationMainPanel.teachingResources" );
//        StringUtils.deleteString( session, "simulationMainPanel.mainTopics" );
//        StringUtils.deleteString( session, "simulationMainPanel.teachingIdeas" );

        StringUtils.addString( session, "simulationMainPanel.about", "About" );
        StringUtils.addString( session, "simulationMainPanel.topics", "Topics" );

        StringUtils.overwriteString( session, "simulationMainPanel.topics", "Content", "Topics" );

        StringUtils.overwriteString( session, "html5.translations-soon", "Our HTML5 simulations are currently English-only. Translations are one of our next steps, and we will contact translators once this is ready. Sorry for the inconvenience!",
                                     "All of our newest sims are written in HTML5 and run in a browser, but are currently only available in English. Enabling translations is one of our next steps. In the meantime, browse all of our translated sims <a {0}>here</a>." );

        StringUtils.addString( session, "simulationMainPanel.description", "Description" );
        StringUtils.addString( session, "simulationMainPanel.forTeachers", "For Teachers" );
        StringUtils.addString( session, "simulationMainPanel.activities", "Activities" );
        StringUtils.addString( session, "simulationMainPanel.translations", "Translations" );

        StringUtils.addString( session, "simulationMainPanel.shareAnActivity", "Share an Activity!" );
        StringUtils.addString( session, "simulationMainPanel.translateThisSim", "Translate this Sim" );
        StringUtils.addString( session, "simulationMainPanel.allSimsIn", "All sims in" );
        StringUtils.addString( session, "simulationMainPanel.translatorInfo", "Find more information <a {0}>for translators</a>" );
        StringUtils.addString( session, "simulationMainPanel.downloadOrRun", "Download or Run" );

        StringUtils.addString( session, "simulationMainPanel.originalSim", "Original Sim and Translations" );
        StringUtils.addString( session, "simulationMainPanel.backToHTML", "Back to HTML5 Version" );

        StringUtils.overwriteString( session, "simulationMainPanel.originalSim", "Original Sim and Translations >>", "Original Sim and Translations"  );
        StringUtils.overwriteString( session, "simulationMainPanel.backToHTML", "Back to HTML5 Version >>", "Back to HTML5 Version" );

        StringUtils.addString( session, "simulationMainPanel.teacherTips", "Teacher Tips" );
        StringUtils.addString( session, "simulationMainPanel.and", "and" );
        StringUtils.addString( session, "simulationMainPanel.videoPrimer", "Video Primer" );
        StringUtils.overwriteString( session, "simulationMainPanel.videoPrimer", "and Video Primer", "Video Primer" );

        StringUtils.addString( session, "simulationMainPanel.signInPrompt", "Please sign in to watch the video primer" );
        StringUtils.addString( session, "simulationMainPanel.noActivities", "Check out <a {0}>activities for the original sim</a> for ideas" );
        StringUtils.addString( session, "simulationMainPanel.moreActivities", "Browse <a {0}>more activities</a>." );

        StringUtils.overwriteString( session, "simulationMainPanel.teachersGuide", "The <a {0} target=\"_blank\">teacher's guide</a> (pdf) contains tips created by the PhET team.", "Overview of sim controls, model simplifications, and insights into student thinking (<a {0} target=\"_blank\">PDF</a>)." );
        StringUtils.overwriteString( session, "simulationMainPanel.teachersGuide", "Overview of sim controls and insights into student thinking (<a {0} target=\"_blank\">Teacher Tips pdf</a>)", "Overview of sim controls, model simplifications, and insights into student thinking (<a {0} target=\"_blank\">PDF</a>)." );

        StringUtils.overwriteString( session, "nav.teacherIdeas", "For Teachers", "Teaching Resources" );
        StringUtils.overwriteString( session, "home.teacherIdeasAndActivities", "For Teachers", "Teaching Resources" );


        StringUtils.overwriteString( session, "html5.crowdfundedProjects", "Support PhET's Annual Campaign:", "Support PhET:" );



        StringUtils.addString( session, "about.accessibility.title", "Accessibility" );
        StringUtils.addString( session, "nav.about.accessibility", "Accessibility" );

        StringUtils.addString( session, "faq.main.q2.answer.java-flash", "<p>The Java and Flash simulations will run on most PC, Mac, and Linux systems. Detailed system requirements for running the original sims are:</p><p><strong>Windows Systems</strong><br/>Intel Pentium processor<br/>Microsoft Windows XP/Vista/7<br/>256MB RAM minimum<br/>Approximately {1} MB available disk space (for full <a {0}>installation</a>)<br/>1024x768 screen resolution or better<br/>Latest version of Oracle Java<br/>Adobe Flash Player 9 or later<br/>Microsoft Internet Explorer 6 or later, Firefox 2 or later</p><p><strong>Macintosh Systems</strong><br/>G3, G4, G5 or Intel processor<br/>OS 10.5 or later<br/>256MB RAM minimum<br/>Approximately {2} MB available disk space (for full <a {0}>installation</a>)<br/>1024x768 screen resolution or better<br/>Latest version of Oracle Java<br/>Adobe Flash Player 9 or later<br/>Safari 2 or later, Firefox 2 or later</p><p><strong>Linux Systems</strong><br/>Intel Pentium processor<br/>256MB RAM minimum<br/>Approximately {3} MB disk space (for full <a {0}>installation</a>)<br/>1024x768 screen resolution or better<br/>Latest version of Oracle Java<br/>Adobe Flash Player 9 or later<br/>Firefox 2 or later<br/></p>" );
        StringUtils.addString( session, "faq.main.q2.answer.java-flash.header", "Java and Flash sims" );
        StringUtils.addString( session, "faq.main.q2.answer.html5.header", "HTML5 sims" );
        StringUtils.addString( session, "faq.main.q2.answer.html5", "<p>The new HTML5 sims can run on iPads, Chromebooks, as well as PC, Mac, and Linux systems.</p>" +
                                                                                "<p><strong>iPad</strong><br/>" +
                                                                                "iOS 7+<br/>" +
                                                                                "Safari<br/>" +
                                                                                "The HTML5 PhET sims are supported on iPad2 or later. For iPad compatible sims <a {0}>click here</a>.<br></p>" +
                                                                                "<p><strong>Android</strong><br/>" +
                                                                                "Not officially supported. Please contact {1} with troubleshooting issues.<br/>" +
                                                                                "If you are using the HTML5 sims on Android, we recommend using the latest version of Google Chrome and Android 4.1+</p>" +
                                                                                "<p><strong>Chromebook</strong><br/>" +
                                                                                "Latest version of Google Chrome<br/>" +
                                                                                "The HTML5 and Flash PhET sims are supported on all Chromebooks. For Chromebook compatible sims <a {2}>click here</a>.</p>" +
                                                                                "<p><strong>Windows Systems</strong><br/>" +
                                                                                "Microsoft Internet Explorer 9 or later, latest version of Firefox, latest version of Google Chrome</p>" +
                                                                                "<p><strong>Macintosh Systems</strong><br/>" +
                                                                                "OS 10.8.5 or later<br/>" +
                                                                                "Safari 6.1+, latest version of Firefox, latest version of Google Chrome</p>" +
                                                                                "<p><strong>Linux Systems</strong><br/>" +
                                                                                "Not officially supported. Please contact {1} with troubleshooting issues.</p>" );

        StringUtils.deleteString( session, "troubleshooting.main.q6.answer" );
        StringUtils.deleteString( session, "faq.mobileDevices.title" );
        StringUtils.deleteString( session, "faq.mobileDevices.answer" );
        StringUtils.deleteString( session, "troubleshooting.main.q1.answer" );
        StringUtils.deleteString( session, "troubleshooting.mobile.p2" );
        StringUtils.deleteString( session, "troubleshooting.mobile.p3" );
        StringUtils.deleteString( session, "troubleshooting.mobile.p4" );
        StringUtils.deleteString( session, "troubleshooting.mobile.p6" );
        StringUtils.addString( session, "troubleshooting.main.q6.answer", "<p>The HTML5 sims are optimized for an iPad2 resolution (1024 x 768 pixels), but work on other resolutions and will scale based on your device" +
                                                                          "          resolution.</p>" +
                                                                          "        <p>The Java and Flash PhET simulations work best at a screen resolution of 1024 x 768 pixels or higher. (Some of them are written so that they" +
                                                                          "          cannot be resized.) At lower resolution (e.g. 800 x 600), all the controls may not fit on your screen. To change your screen resolution," +
                                                                          "          follow the directions below:</p>" +
                                                                          "        <p><strong>Windows 7/8</strong><br/>" +
                                                                          "        <ol>" +
                                                                          "          <li>From Start menu, click on \"Control Panel.\"</li>" +
                                                                          "          <li>Choose \"Display\".</li>" +
                                                                          "          <li>Press \"Adjust screen resolution\".</li>" +
                                                                          "          <li>Use the \"Resolution\" slider to select a resolution and click \"OK.\"</li>" +
                                                                          "        </ol></p>" +
                                                                          "        <p><strong>Windows Vista</strong><br/>" +
                                                                          "        <ol>" +
                                                                          "          <li>From Start menu, click on \"Control Panel.\"</li>" +
                                                                          "          <li>Press \"Adjust screen resolution\" under \"Appearance and Personalization.\"</li>" +
                                                                          "          <li>Use the \"Screen resolution\" slider to select a resolution and click \"OK.\"</li>" +
                                                                          "        </ol></p>" +
                                                                          "        <p><strong>Windows 2000/XP</strong><br/>" +
                                                                          "        <ol>" +
                                                                          "          <li>From Start menu, click on \"Control Panel.\"</li>" +
                                                                          "          <li>Double click on \"Display\" icon.</li>" +
                                                                          "          <li>Select the \"Settings\" tab.</li>" +
                                                                          "          <li>Use the \"Screen resolution\" slider to select a resolution and click \"OK.\"</li>" +
                                                                          "        </ol></p>" +
                                                                          "        <p><strong>Macintosh</strong><br/>" +
                                                                          "        <ol>" +
                                                                          "          <li>Open the System Preferences (either from the Dock or from the Apple menu).</li>" +
                                                                          "          <li>Open the Displays Panel and choose the Display tab.</li>" +
                                                                          "          <li>On the left of the Displays tab you can select one of the Resolutions from the list.</li>" +
                                                                          "        </ol></p>" );
        StringUtils.addString( session, "faq.mobileDevices.title" , "Can PhET simulations be used on my tablet/iPad/Android/Chromebook devices?" );
        StringUtils.addString( session, "faq.mobileDevices.answer" , "<p>" +
                                                                 "          The Java and Flash simulations cannot run on a tablet/iPad/Android device because those devices do not fully support Java or Flash, which is" +
                                                                 "          required to run the Java and Flash PhET sims." +
                                                                 "        </p>" +
                                                                 "            <p><strong>iPad</strong><br/>" +
                                                                 "              The HTML5 PhET sims are supported on iPad2 or later. For iPad compatible sims" +
                                                                 "              <a {0}>click here</a>." +
                                                                 "            </p>" +
                                                                 "            <p><strong>Chromebook</strong><br/>" +
                                                                 "              The HTML5 and Flash PhET sims are supported on all Chromebooks using the latest version of Google Chrome. For Chromebook compatible sims" +
                                                                 "              <a {1}>click here</a>." +
                                                                 "            </p>" +
                                                                 "            <p><strong>Android</strong><br/>" +
                                                                 "              We do not currently test our simulations on Android devices due to the fragmented nature of the device ecosystem. However, the " +
                                                                 "                <a {0}>HTML5 sims</a> <i>should</i> function. For optimal" +
                                                                 "              performance, we recommend opening the sims in the latest version of Google Chrome." +
                                                                 "            </p>" +
                                                                 "        <p>" +
                                                                 "          If your school district is considering the purchase of iPads or tablet devices, please understand that you will be limited to HTML5" +
                                                                 "          sims. For more information, see our <a {2}>system requirements</a>." +
                                                                 "        </p>" );
        StringUtils.addString( session, "troubleshooting.main.q1.answer", "<p>The original sims are programmed in Java or Flash. Flash is installed in most modern desktop web browsers while Java is a free application that can be downloaded from Oracle. To run the Java-based simulations you must have Java version 1.5 or higher installed on your computer." +
                                                                          "        To download the latest version of Java, please visit: {0}.</p>" +
                                                                          "<p>" +
                                                                          "        If you are running the sims on a tablet or Chromebook <a {1}>see here info on compatible sims</a>." +
                                                                          "      </p>" );
        StringUtils.addString( session, "troubleshooting.mobile.p2", "See our new HTML5 sims here: <a href=\"{0}\">http://phet.colorado.edu{0}</a>." );
        StringUtils.addString( session, "troubleshooting.mobile.p3", "Or watch a video about our new HTML5 efforts: {0}." );
        StringUtils.addString( session, "troubleshooting.mobile.p4", "Additionally, Chromebooks can run our Flash sims. For a list of Chromebook-compatible sims: <a href=\"{0}\">http://phet.colorado.edu{0}</a>." );
        StringUtils.addString( session, "troubleshooting.mobile.p6", "Our plan is to port most of the sims eventually to HTML5, but there are likely some less popular sims that will not be ported. It may be" +
                                                                     "        several years before the majority of PhET sims are converted to HTML5 (we have been publishing ~1 sim per month). This is because porting the" +
                                                                     "        sims to HTML5 is very expensive for us - it basically involves redesigning the sim for touch, rewriting all of the sim and model code, then" +
                                                                     "        testing on an array of devices." );

        StringUtils.overwriteString( session, "contribution.view.addComment", "Add Comment", "Comment" );
        StringUtils.overwriteString( session, "contribution.view.simulations", "Simulations", "Simulation(s)" );
        StringUtils.overwriteString( session, "contribution.view.authors", "Authors", "Author(s)" );
        StringUtils.overwriteString( session, "contribution.view.zipDownload", "Or you may <a {0}>download</a> all " +
                "files as a compressed zip archive.", "<a {0}>Download</a> all files as a compressed .zip" );
        StringUtils.overwriteString( session, "contribution.view.submitted", "Submitted", "Date submitted" );
        StringUtils.overwriteString( session, "contribution.view.updated", "Updated", "Date updated" );
        session.close();
    }

    /*---------------------------------------------------------------------------*
    * deprecated strings:
    * newsletter-instructions
    * about.who-we-are.title
    *----------------------------------------------------------------------------*/

}
