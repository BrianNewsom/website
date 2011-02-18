/*
 * Copyright 2011, University of Colorado
 */

package edu.colorado.phet.website.cache;

import java.util.HashMap;

import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostUpdateEvent;

import edu.colorado.phet.website.data.Project;
import edu.colorado.phet.website.data.Simulation;
import edu.colorado.phet.website.data.util.HibernateEventListener;
import edu.colorado.phet.website.data.util.IChangeListener;
import edu.colorado.phet.website.util.WebImage;

/**
 * Caches image objects based on their src attribute
 */
public class ImageCache {
    private static HashMap<String, WebImage> imageMap = new HashMap<String, WebImage>();

    static {
        // we need to initialize some listeners so that on any project / sim change we invalidate all images.
        // hopefully for now those are the only mutable Apache images that we will deal with

        IChangeListener anyChangeInvalidator = new IChangeListener() {
            public void onInsert( Object object, PostInsertEvent event ) {
                invalidate();
            }

            public void onUpdate( Object object, PostUpdateEvent event ) {
                invalidate();
            }

            public void onDelete( Object object, PostDeleteEvent event ) {
                invalidate();
            }

            public void onCollectionUpdate( Object object, PostCollectionUpdateEvent event ) {
                invalidate();
            }
        };
        HibernateEventListener.addListener( Project.class, anyChangeInvalidator );
        HibernateEventListener.addListener( Simulation.class, anyChangeInvalidator );
    }

    public static synchronized void invalidate() {
        imageMap.clear();
    }

    public static synchronized WebImage get( String src ) {
        return imageMap.get( src );
    }

    public static synchronized void set( String src, WebImage image ) {
        imageMap.put( src, image );
    }
}