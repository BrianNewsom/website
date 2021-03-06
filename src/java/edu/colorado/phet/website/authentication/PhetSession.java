/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.authentication;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebSession;
import org.hibernate.Query;

import edu.colorado.phet.website.data.PhetUser;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;
import edu.colorado.phet.website.util.hibernate.Result;
import edu.colorado.phet.website.util.hibernate.VoidTask;
import edu.colorado.phet.website.util.hibernate.Task;
import edu.colorado.phet.website.util.hibernate.TaskException;

public class PhetSession extends WebSession {

    private boolean signedIn = false;
    private PhetUser user = null;

    private static final Logger logger = Logger.getLogger( PhetSession.class.getName() );

    public static PhetSession get() {
        return (PhetSession) Session.get();
    }

    public PhetSession( Request request ) {
        super( request );
    }

    public static boolean passwordEquals( PhetUser user, String password ) {
        if ( user.getHashedPassword().equals( compatibleHashPassword( password ) ) ) {
            return true;
        }
        else {
            return user.getHashedPassword().equals( saltedPassword( user.getEmail(), password ) );
        }
    }

    /**
     * Sets the user that is logged in, or null if nobody is logged in.
     *
     * @param user
     */
    public void setUser( PhetUser user ) {//note: this is not symmetric with signOut
        this.user = user;
        signedIn = ( user != null );
    }

    public void signOut() {
        setUser( null );
        invalidateNow();
    }

    public boolean signIn( PhetRequestCycle currentCycle, final String username, final String password ) {
        final PhetUser user = getAuthenticatedUser( currentCycle, username, password );
        setUser( user );
        return isSignedIn();
    }

    public boolean signInWithoutPassword( PhetRequestCycle currentCycle, final int userId ) {
        return HibernateUtils.wrapCatchTransaction( currentCycle.getHibernateSession(), new VoidTask() {
            public void run( org.hibernate.Session session ) {
                PhetUser user = (PhetUser) session.load( PhetUser.class, userId );
                setUser( user );
            }
        } ) && isSignedIn(); // note: relying on order of evaulation L->R. says OK: http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    private PhetUser getAuthenticatedUser( PhetRequestCycle currentCycle, final String username, final String password ) {
        Result<PhetUser> result = HibernateUtils.resultCatchTransaction( currentCycle.getHibernateSession(), new Task<PhetUser>() {
            public PhetUser run( org.hibernate.Session session ) {

                String depreciatedHash = compatibleHashPassword( password ); // old method of hashing without salt
                String saltedHash = saltedPassword( username, password );    // new method of hashing with salt

                // TODO: possibly create an index for this operation?
                // first try to find the user with the new style of hashed password with a salt
                Query query = session.createQuery( "select u from PhetUser as u where (u.email = :email and u.hashedPassword = :compatiblePassword)" );
                query.setString( "email", username );
                query.setString( "compatiblePassword", saltedHash );

                // if that doesn't find a user, try the depreciated version of hashing
                PhetUser user = (PhetUser) query.uniqueResult();
                if ( user == null ) {
                    Query query2 = session.createQuery( "select u from PhetUser as u where (u.email = :email and u.hashedPassword = :compatiblePassword)" );
                    query2.setString( "email", username );
                    query2.setString( "compatiblePassword", depreciatedHash );
                    user = (PhetUser) query2.uniqueResult();

                    // if that doesn't work, the user isn't found
                    if ( user == null ) {
                        throw new TaskException( "User does not exist", Level.DEBUG );
                    }
                    // if the user is found using the old style of hashing, update their hashed password
                    else
                    {
                        user.setHashedPassword( saltedHash );
                        session.update( user );
                    }
                }
                user.ensureHasConfirmationKey( session );
                if ( !user.isConfirmed() ) {
                    logger.info( "User " + user.getEmail() + " has not confirmed their email" );
                    return null; // will not log in an unconfirmed user
                }
                return user;
            }
        } );
        return result.value;
    }

    public PhetUser getUser() {
        return user;
    }

    public static String compatibleHashPassword( final String password ) {
        byte[] bytes;
        try {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            digest.reset();
            digest.update( ( password + "_phetx1225" ).getBytes( "UTF-8" ) );
            bytes = digest.digest();
        }
        catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            throw new RuntimeException( "No such algorithm", e );
        }
        catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }

        //return base64Encode( new String( bytes ) );
        return hexEncode( bytes );
    }

    public static String saltedPassword( final String email, final String password ) {
        byte[] bytes;
        String salt = email.substring( 0, 5 );
        try {
            MessageDigest digest = MessageDigest.getInstance( "MD5" );
            digest.reset();
            digest.update( ( password + salt ).getBytes( "UTF-8" ) );
            bytes = digest.digest();
        }
        catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            throw new RuntimeException( "No such algorithm", e );
        }
        catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        }

        return hexEncode( bytes );
    }

    private static String hexEncode( byte[] bytes ) {
        StringBuffer buffer = new StringBuffer();
        for ( byte b : bytes ) {
            buffer.append( byteToHex( b ) );
        }
        return buffer.toString();
    }

    private static String byteToHex( byte b ) {
        char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] array = { hexDigit[( b >> 4 ) & 0x0f], hexDigit[b & 0x0f] };
        return new String( array );
    }

    private static String base64Data = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


    private static String base64Encode( String string ) {
        String ret = "";
        int count = ( 3 - ( string.length() % 3 ) ) % 3;
        string += "\0\0".substring( 0, count );
        for ( int i = 0; i < string.length(); i += 3 ) {
            int j = ( string.charAt( i ) << 16 ) + ( string.charAt( i + 1 ) << 8 ) + string.charAt( i + 2 );
            ret = ret + base64Data.charAt( ( j >> 18 ) & 0x3f ) + base64Data.charAt( ( j >> 12 ) & 0x3f ) + base64Data.charAt( ( j >> 6 ) & 0x3f ) + base64Data.charAt( j & 0x3f );
        }
        return ret.substring( 0, ret.length() - count ) + "==".substring( 0, count );
    }

    private static class Test {
        public static void main( String[] args ) {
            System.out.println( compatibleHashPassword( args[0] ) );
        }
    }
}
