/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.newsletter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

import edu.colorado.phet.common.phetcommon.util.FileUtils;
import edu.colorado.phet.common.phetcommon.util.FunctionalUtils;
import edu.colorado.phet.common.phetcommon.util.function.Function1;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.data.PhetUser;
import edu.colorado.phet.website.util.EmailUtils;
import edu.colorado.phet.website.util.PageContext;

public class NewsletterSender {

    // TODO: add dev flag

    private String rawBody;
    private String rawText;
    private String subject;
    private String fromAddress;
    private String replyTo;
    private Map<String,Boolean> ignoreEmails;

    // whether newsletters should also be sent when users subscribe
    private boolean automated;

//    private List<File> images = new LinkedList<File>();

    private static boolean sending = false;
    private static final Object lock = new Object();

    private static final Logger logger = Logger.getLogger( NewsletterSender.class );

    public NewsletterSender() {
        try {
            File newsletterPropertiesFile = PhetWicketApplication.get().getWebsiteProperties().getNewsletterFile();

            Properties properties = new Properties();
            FileInputStream in = new FileInputStream( newsletterPropertiesFile );
            try {
                properties.load( in );
            }
            finally {
                in.close();
            }
            subject = properties.getProperty( "subject" );
            fromAddress = properties.getProperty( "fromAddress" );
            replyTo = properties.getProperty( "replyTo" );
            automated = properties.getProperty( "automated" ).equals( "true" );
            rawText = FileUtils.loadFileAsString( new File( properties.getProperty( "bodyPlainTextFile" ) ) );
            rawBody = FileUtils.loadFileAsString( new File( properties.getProperty( "bodyFile" ) ) );

            // allow ignoring certain emails
            File ignoreListFile = new File( properties.getProperty( "ignore-list" ) );
            ignoreEmails = new HashMap<String,Boolean>();
            if ( ignoreListFile.exists() ) {
                for ( String email : Arrays.asList( FileUtils.loadFileAsString( ignoreListFile ).split( "\n" ) ) ) {
                    ignoreEmails.put( email, true );
                }
            }

//            for ( String imageFilename : properties.getProperty( "images" ).split( " " ) ) {
//                File imageFile = new File( imageFilename );
//                if ( !imageFile.exists() ) {
//                    throw new FileNotFoundException( "image file not found: " + imageFilename );
//                }
//                images.add( imageFile );
//            }
        }
        catch ( FileNotFoundException e ) {
            logger.error( "message prep error: ", e );
        }
        catch ( IOException e ) {
            logger.error( "message prep error: ", e );
        }
    }

    public boolean allowAutomatedNewsletterEmails() {
        return automated;
    }

    public boolean sendNewsletters( List<PhetUser> users ) {
        // filter out user emails that are to be ignored
        users = FunctionalUtils.filter( users, new Function1<PhetUser, Boolean>() {
            public Boolean apply( PhetUser phetUser ) {
                return !ignoreEmails.containsKey( phetUser.getEmail() );
            }
        } );

        // sort all of them
        Collections.sort( users, new Comparator<PhetUser>() {
            public int compare( PhetUser a, PhetUser b ) {
                return a.getEmail().compareTo( b.getEmail() );
            }
        } );
        synchronized ( lock ) {
            if ( sending ) {
                return false; // exit immediately. don't double-send
            }
            sending = true;
        }
        for ( PhetUser user : users ) {
            if ( user.isConfirmed() && user.isReceiveEmail() && PhetUser.isValidEmail( user.getEmail() ) ) {
                sendNewsletter( user );

                // throttle newsletter sending
                try {
                    Thread.sleep( 1500 );
                }
                catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }
        }
        synchronized ( lock ) {
            sending = false;
        }
        return true;
    }

    private boolean sendNewsletter( PhetUser user ) { // TODO: improve signature
        logger.info( "sending newsletter to " + user.getEmail() );
        try {
            EmailUtils.GeneralEmailBuilder message = new EmailUtils.GeneralEmailBuilder( subject, fromAddress );
            message.setFromName( "PhET Interactive Simulations" );
            String link = NewsletterUtils.getUnsubscribeLink( PageContext.getNewDefaultContext(), user.getConfirmationKey() );
            String body = rawBody;
            body = FileUtils.replaceAll( body, "@UNSUBSCRIBE@", link );
            body = FileUtils.replaceAll( body, "@NAME@", user.getName() == null ? "you" : user.getName() );
            String textBody = rawText;
            textBody = FileUtils.replaceAll( textBody, "@UNSUBSCRIBE@", link );
            textBody = FileUtils.replaceAll( textBody, "@NAME@", user.getName() == null ? "you" : user.getName() );
            message.setBody( body );
            message.setPlainTextAlternative( textBody );
            message.addRecipient( user.getEmail().trim() );
            message.addReplyTo( replyTo );
//            for ( final File imageFile : images ) {
//                message.addBodyPart( new MimeBodyPart() {{
//                    setDataHandler( new DataHandler( new FileDataSource( imageFile ) ) );
//                    setHeader( "Content-ID", "<" + imageFile.getName() + ">" );
//                }} );
//            }
            return EmailUtils.sendMessage( message );
        }
        catch ( MessagingException e ) {
            logger.warn( "message send error: ", e );
            return false;
        }
    }
}
