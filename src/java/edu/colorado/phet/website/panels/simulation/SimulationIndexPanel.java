/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.panels.simulation;

import java.text.Collator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import edu.colorado.phet.website.content.simulations.SimulationPage;
import edu.colorado.phet.website.data.LocalizedSimulation;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

public class SimulationIndexPanel extends PhetPanel {
    private List<String> letters;

    public SimulationIndexPanel( String id, final PageContext context, List<LocalizedSimulation> simulations ) {
        super( id, context );

        final Map<String, List<LocalizedSimulation>> lettermap = new HashMap<String, List<LocalizedSimulation>>();
        letters = new LinkedList<String>();

        for ( LocalizedSimulation simulation : simulations ) {
            String name = simulation.getTitle();
            String chr = HibernateUtils.getLeadingSimCharacter( name, context.getLocale() );
            if ( lettermap.containsKey( chr ) ) {
                if ( !lettermap.get( chr ).contains( simulation ) ) {
                    lettermap.get( chr ).add( simulation );
                }
            }
            else {
                List<LocalizedSimulation> li = new LinkedList<LocalizedSimulation>();
                li.add( simulation );
                lettermap.put( chr, li );
                letters.add( chr );
            }
        }

        Collections.sort( letters, Collator.getInstance( getLocale() ) );

        add( new ListView<String>( "sim-group", letters ) {
            protected void populateItem( ListItem<String> item ) {
                String letter = item.getModelObject();
                item.setOutputMarkupId( true );
                item.setMarkupId( HibernateUtils.encodeCharacterId( letter ) );
                item.add( new Label( "letter", letter ) );
                item.add( new ListView<LocalizedSimulation>( "sim-entry", lettermap.get( letter ) ) {
                    protected void populateItem( ListItem<LocalizedSimulation> subItem ) {
                        LocalizedSimulation simulation = subItem.getModelObject();
                        Link link = SimulationPage.getLinker( simulation ).getLink( "sim-link", context, getPhetCycle() );
                        link.add( new Label( "sim-title", simulation.getTitle() ) );
                        subItem.add( link );
                    }
                } );
            }
        } );

        //add( HeaderContributor.forCss( CSS.SIMULATION_INDEX ) );

    }

    public List<String> getLetters() {
        return letters;
    }
}