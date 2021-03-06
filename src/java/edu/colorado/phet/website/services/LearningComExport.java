/*
 * Copyright 2011, University of Colorado
 */

package edu.colorado.phet.website.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.hibernate.Session;

import edu.colorado.phet.website.components.RawLabel;
import edu.colorado.phet.website.data.Category;
import edu.colorado.phet.website.data.GradeLevel;
import edu.colorado.phet.website.data.Keyword;
import edu.colorado.phet.website.data.Simulation;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.RawCSV;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;
import edu.colorado.phet.website.util.hibernate.VoidTask;

/**
 * CSV of simulation data that is targetted towards exporting data to learning.com
 */
public class LearningComExport extends WebPage {

    private RawCSV csv;

    public LearningComExport( PageParameters parameters ) {
        super( parameters );

        csv = new RawCSV();

        // column headers
        csv.addColumnValue( "Simulation Name" ); // simulation name
        csv.addColumnValue( "Product High Grade" );
        csv.addColumnValue( "Product Low Grade" );
        csv.addColumnValue( "Sequence Title" ); // like "Physics"?
        csv.addColumnValue( "Unit Title" ); // like sub-subject ("Motion")?
        csv.addColumnValue( "Curriculum Item Type Icon" ); // thumbnail
        csv.addColumnValue( "Curriculum Item Description" ); // sim description
        csv.addColumnValue( "Direct Item Link" ); // link to "Run Now"
        csv.addColumnValue( "Keywords" ); // main topics + keywords, delimited by " | "
        csv.addColumnValue( "Primary Learning Objective" ); // learning goals
        csv.betweenLines();

        // simulations
        HibernateUtils.wrapTransaction( PhetRequestCycle.get().getHibernateSession(), new VoidTask() {
            public void run( final Session session ) {
                List list = session.createQuery( "select s from Simulation as s" ).list();
                List<Simulation> simulations = new LinkedList<Simulation>( list );

                // sort by project name, then by simulation name
                Collections.sort( simulations, new Comparator<Simulation>() {
                    // TODO: replace with StringComparator once we merge
                    public String toString( Simulation simulation ) {
                        return simulation.getProject().getName() + " " + simulation.getName();
                    }

                    public int compare( Simulation a, Simulation b ) {
                        return toString( a ).compareTo( toString( b ) );
                    }
                } );
                for ( final Simulation simulation : simulations ) {
                    if ( !simulation.isVisible() ) {
                        continue;
                    }
                    csv.addColumnValue( simulation.getEnglishSimulation().getTitle() ); // simulation name

                    Set<String> mainCategories = new HashSet<String>();
                    Set<String> parentCategories = new HashSet<String>();

                    for ( Object o : simulation.getCategories() ) {
                        Category category = (Category) o;
                        if ( !category.isGradeLevelCategory() ) {
                            mainCategories.add( StringUtils.lookup( session, category.getLocalizationKey() ) );
                            if ( !category.getParent().isRoot() ) {
                                parentCategories.add( StringUtils.lookup( session, category.getParent().getLocalizationKey() ) );
                            }
                        }
                    }

                    csv.addColumnValue( getHighGradeLevelString( simulation.getMaxGradeLevel() ) );// high grade level
                    csv.addColumnValue( getLowGradeLevelString( simulation.getMinGradeLevel() ) );// low grade level

                    csv.addColumnValue( joinDelimited( parentCategories ) ); // sequence title
                    csv.addColumnValue( joinDelimited( mainCategories ) ); // unit title

                    csv.addColumnValue( StringUtils.makeUrlAbsolute( simulation.getThumbnailUrl() ) ); // thumbnail url
                    csv.addColumnValue( StringUtils.lookup( session, simulation.getDescriptionKey() ) ); // English desc
                    csv.addColumnValue( StringUtils.makeUrlAbsolute( simulation.getEnglishSimulation().getRunUrl() ) ); // direct item link

                    /*---------------------------------------------------------------------------*
                    * " | "-delimited keywords
                    *----------------------------------------------------------------------------*/

                    final Set<String> keywordKeys = new HashSet<String>();
                    for ( Keyword keyword : new HashSet<Keyword>() {{
                        addAll( simulation.getKeywords() );
                        addAll( simulation.getTopics() );
                    }} ) {
                        keywordKeys.add( keyword.getLocalizationKey() );
                    }
                    List<String> keywords = new LinkedList<String>() {{
                        for ( String keywordKey : keywordKeys ) {
                            add( StringUtils.lookup( session, keywordKey ) );
                        }
                        Collections.sort( this );
                    }};
                    csv.addColumnValue( joinDelimited( keywords ) );

                    // learning goals
                    csv.addColumnValue( joinDelimited( Arrays.asList( StringUtils.lookup( session, simulation.getLearningGoalsKey() ).split( "<br/>" ) ) ) );

                    csv.betweenLines();
                }
            }
        } );

        add( new RawLabel( "text", csv.toString() ) {{
            setRenderBodyOnly( true );
        }} );

        getResponse().setContentType( "text/csv" );
    }

    private String getHighGradeLevelString( GradeLevel level ) {
        switch( level ) {
            case ELEMENTARY_SCHOOL:
                return "5";
            case MIDDLE_SCHOOL:
                return "8";
            case HIGH_SCHOOL:
                return "12";
            case UNIVERSITY:
                return "U";
            default:
                return "?";
        }
    }

    private String getLowGradeLevelString( GradeLevel level ) {
        switch( level ) {
            case ELEMENTARY_SCHOOL:
                return "4";
            case MIDDLE_SCHOOL:
                return "6";
            case HIGH_SCHOOL:
                return "9";
            case UNIVERSITY:
                return "U";
            default:
                return "?";
        }
    }

    private static String joinDelimited( Collection<String> strings ) {
        String result = "";
        for ( String string : strings ) {
            if ( result.length() > 0 ) {
                result += " | ";
            }
            result += string;
        }
        return result;
    }

}