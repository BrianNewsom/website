/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.data.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

public class HibernateEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener, PostCollectionUpdateEventListener {

    private static final Logger logger = Logger.getLogger( HibernateEventListener.class.getName() );

    private static Map<Class, List<IChangeListener>> listenermap = new HashMap<Class, List<IChangeListener>>();

    private static HibernateEventListener me;

    public HibernateEventListener() {
        logger.info( "Creating HibernateEventListener" );
        synchronized( this ) {
            me = this;
        }
    }

    public static synchronized int getListenerCount() {
        int count = 0;
        for ( List<IChangeListener> listeners : listenermap.values() ) {
            count += listeners.size();
        }
        return count;
    }

    public static synchronized String getListenerReport() {
        Map<String, Integer> keyHistogram = new HashMap<String, Integer>();
        Map<String, Integer> valueHistogram = new HashMap<String, Integer>();

        for ( Class clazz : listenermap.keySet() ) {
            List<IChangeListener> listeners = listenermap.get( clazz );
            keyHistogram.put( clazz.getName(), listeners.size() );

            for ( IChangeListener listener : listeners ) {
                String listenerClassName = listener.getClass().getName();
                if ( valueHistogram.containsKey( listenerClassName ) ) {
                    valueHistogram.put( listenerClassName, valueHistogram.get( listenerClassName ) + 1 );
                }
                else {
                    valueHistogram.put( listenerClassName, 1 );
                }
            }
        }

        StringBuilder builder = new StringBuilder(  );

        builder.append( "Hibernate Event classes:<br>" );

        for ( String key : keyHistogram.keySet() ) {
            builder.append( key ).append( ": " ).append( keyHistogram.get( key ) );
        }

        builder.append( "<br><br>Hibernate listener classes:<br>" );

        for ( String key : valueHistogram.keySet() ) {
            builder.append( key ).append( ": " ).append( valueHistogram.get( key ) );
        }

        return builder.toString();
    }

    public static synchronized void addListener( Class eClass, IChangeListener listener ) {
        List<IChangeListener> listeners = listenermap.get( eClass );
        if ( listeners == null ) {
            listeners = new LinkedList<IChangeListener>();
            listenermap.put( eClass, listeners );
        }
        listeners.add( listener );
    }

    public static synchronized void removeListener( Class eClass, IChangeListener listener ) {
        List<IChangeListener> listeners = listenermap.get( eClass );
        if ( listeners == null ) {
            throw new RuntimeException( "No listeners registered for that class" );
        }
        else {
            listeners.remove( listener );
        }
    }

    public synchronized void onPostInsert( PostInsertEvent event ) {
        Object entity = event.getEntity();
        Class eClass = entity.getClass();
        List<IChangeListener> listeners = listenermap.get( eClass );
        logger.debug( "post-insert on " + eClass.getSimpleName() + ", notifying " + ( listeners == null ? 0 : listeners.size() ) );
        if ( listeners != null ) {
            List<IChangeListener> copy = new LinkedList<IChangeListener>( listeners );
            for ( IChangeListener listener : copy ) {
                listener.onInsert( entity, event );
            }
        }
    }

    public synchronized void onPostUpdate( PostUpdateEvent event ) {
        Object entity = event.getEntity();
        Class eClass = entity.getClass();

        List<IChangeListener> listeners = listenermap.get( eClass );
        logger.debug( "post-update on " + eClass.getSimpleName() + ", notifying " + ( listeners == null ? 0 : listeners.size() ) );
        if ( listeners != null ) {
            List<IChangeListener> copy = new LinkedList<IChangeListener>( listeners );
            for ( IChangeListener listener : copy ) {
                listener.onUpdate( entity, event );
            }
        }

        if ( entity instanceof DataListener ) {
            ( (DataListener) entity ).onUpdate();
        }

//        logger.debug( "----" );
//        logger.debug( eClass.getCanonicalName() );
//        logger.debug( "id: " + event.getId() );
//        logger.debug( "name: " + event.getPersister().getPropertyNames()[1] );
//        logger.debug( "lazy: " + event.getPersister().getPropertyLaziness()[1] );
//        logger.debug( "old: " + event.getOldState()[1] );
//        logger.debug( "new: " + event.getState()[1] );


    }

    public synchronized void onPostDelete( PostDeleteEvent event ) {
        Object entity = event.getEntity();
        Class eClass = entity.getClass();
        List<IChangeListener> listeners = listenermap.get( eClass );
        logger.debug( "post-delete on " + eClass.getSimpleName() + ", notifying " + ( listeners == null ? 0 : listeners.size() ) );
        if ( listeners != null ) {
            List<IChangeListener> copy = new LinkedList<IChangeListener>( listeners );
            for ( IChangeListener listener : copy ) {
                listener.onDelete( entity, event );
            }
        }
    }

    public static int getPersisterIndex( EntityPersister persister, String name ) {
        String[] names = persister.getPropertyNames();
        for ( int i = 0; i < names.length; i++ ) {
            if ( names[i].equals( name ) ) {
                // no current check for lazyness, but can do persister.getPropertyLaziness()[i]
                return i;
            }
        }
        return -1;
    }

    public static boolean getSafeHasChanged( PostUpdateEvent event, String name ) {
        int idx = getPersisterIndex( event.getPersister(), name );
        if ( idx == -1 ) {
            // name not included, so maybe it changed? warning!
            logger.warn( "could not find parameter " + name + " in PostUpdateEvent for " + event.getEntity().getClass().getCanonicalName() );
            return true;
        }
        Object oldVal = event.getOldState()[idx];
        Object newVal = event.getState()[idx];
//        System.out.println( name + ": " + oldVal + " -> " + newVal ); // TODO: why was this reporting visible: true -> true ?
        if ( oldVal == null ) {
            return newVal != null;
        }
        else {
            return !oldVal.equals( newVal );
        }
    }

    public static synchronized HibernateEventListener get() {
        return me;
    }

    public void onPostUpdateCollection( PostCollectionUpdateEvent event ) {
        Object entity = event.getAffectedOwnerOrNull();
        Class eClass = entity.getClass();
        List<IChangeListener> listeners = listenermap.get( eClass );
        logger.debug( "post-collection-update on " + eClass.getSimpleName() + ", notifying " + ( listeners == null ? 0 : listeners.size() ) );
        if ( listeners != null ) {
            List<IChangeListener> copy = new LinkedList<IChangeListener>( listeners );
            for ( IChangeListener listener : copy ) {
                listener.onCollectionUpdate( entity, event );
            }
        }
    }
}
