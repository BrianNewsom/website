package edu.colorado.phet.website.menu;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.Link;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.links.Linkable;

public class NavLocation implements Serializable {

    private String key;
    private List<NavLocation> children = new LinkedList<NavLocation>();
    private NavLocation parent;
    private transient Linkable linker;

    /**
     * Whether the location will be hidden if NOT selected
     */
    private boolean hidden = false;

    private static Logger logger = Logger.getLogger( NavLocation.class.getName() );

    public NavLocation( NavLocation parent, String key, Linkable linker ) {
        this.parent = parent;
        this.key = key;
        this.linker = linker;
    }

    public String getLocalizationKey() {
        if ( getKey().startsWith( "keyword" ) || getKey().startsWith( "language.names." ) ) {
            return getKey();
        }
        else {
            return "nav." + getKey();
        }
    }

    public String getBaseKey() {
        if ( getParent() == null ) {
            return getKey();
        }
        else {
            return getParent().getBaseKey();
        }
    }

    public boolean isUnderLocation( NavLocation location ) {
        if ( location == this ) {
            return true;
        }
        else if ( getParent() == null ) {
            return false;
        }
        else {
            return getParent().isUnderLocation( location );
        }
    }

    public boolean isUnderLocationKey( String key ) {
        if ( getKey().equals( key ) ) {
            return true;
        }
        if ( getParent() == null ) {
            return false;
        }
        else {
            return getParent().isUnderLocationKey( key );
        }
    }

    @Override
    public String toString() {
        return "NL: " + getKey();
    }

    public String getKey() {
        return key;
    }

    public List<NavLocation> getChildren() {
        return children;
    }

    public List<NavLocation> getVisibleChildren( Collection<NavLocation> selectedLocations ) {
        List<NavLocation> ret = new LinkedList<NavLocation>();
        for ( NavLocation child : children ) {
            if ( child.isHidden() ) {
                boolean skip = true;
                for ( NavLocation location : selectedLocations ) {
                    if ( location.isUnderLocation( child ) ) {
                        skip = false;
                        break;
                    }
                }
                if ( !skip ) {
                    ret.add( child );
                }
            }
            else {
                ret.add( child );
            }
        }
        return ret;
    }

    public NavLocation getParent() {
        return parent;
    }

    public Linkable getLinker() {
        return linker;
    }

    public Link getLink( String id, PageContext context, PhetRequestCycle cycle ) {
        return linker.getLink( id, context, cycle );
    }

    public void addChild( NavLocation location ) {
        children.add( location );
    }

    public void removeChild( NavLocation location ) {
        children.remove( location );
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden( boolean hidden ) {
        this.hidden = hidden;
    }

    public void organizeSimulationLocations() {
        if ( !key.equals( "simulations" ) ) {
            logger.warn( "attempting to organize non-simulations category" );
            return;
        }
        final List<NavLocation> lowLocations = PhetWicketApplication.get().getMenu().getLocationsBelowCategories();
        Collections.sort( children, new Comparator<NavLocation>() {
            public int compare( NavLocation a, NavLocation b ) {
                boolean ax = lowLocations.contains( a );
                boolean bx = lowLocations.contains( b );
                if( ax == bx ) {
                    return 0;
                }
                if( ax ) {
                    return 1;
                } else {
                    return -1;
                }
            }
        } );
    }
}
