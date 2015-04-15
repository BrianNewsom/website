/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Session;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.constants.Licenses;
import edu.colorado.phet.website.content.simulations.AbstractSimulationPage;
import edu.colorado.phet.website.data.faq.FAQList;
import edu.colorado.phet.website.data.util.IntId;
import edu.colorado.phet.website.metadata.LRETerm;
import edu.colorado.phet.website.util.UrlUtils;
import edu.colorado.phet.website.util.WebImage;

public class Simulation implements Serializable, IntId {
    private int id;
    private String name;
    private Project project;

    private int kilobytes;
    private Set localizedSimulations = new HashSet(); // type LocalizedSimulation
    private Set categories = new HashSet(); // type Category
    private Set alignments = new HashSet(); // type Alignment
    private Set secondaryAlignments = new HashSet(); // type Alignment
    private Set contributions = new HashSet(); // type Contributuion. user-contributed activities
    private Set scienceLiteracyMapKeys = new HashSet(); // type String. keys for the NSDL science literacy maps
    private Set lreTermIDs = new HashSet(); // type String. IDs of the LRE-0001 metadata terms related to this simulation

    private List topics = new LinkedList(); // type Keyword
    private List keywords = new LinkedList(); // type Keyword

    private List relatedSimulations = new LinkedList(); // type Simulation

    private FAQList faqList; // FAQs, if available

    private String designTeam;
    private String libraries;
    private String thanksTo;
    private String videoUrl;

    private boolean underConstruction;
    private boolean guidanceRecommended;
    private boolean classroomTested;
    private boolean simulationVisible;
    private boolean hasCreativeCommonsAttributionLicense;
    private boolean faqVisible;

    /**
     * What the simulation ID was on the old PHP site. Kept for redirections, etc.
     */
    private int oldId;

    private Date createTime; // when simulation was uploaded initially to the server
    private Date updateTime; // whenever something about the simulation object is updated. doesn't mean the sim itself changed, but possibly metadata

    private GradeLevel lowGradeLevel; // typical low-end grade level for usage
    private GradeLevel highGradeLevel; // typical high-end grade level for usage

    public static final String DEFAULT_DESCRIPTION = "Description coming soon";
    public static final String DEFAULT_LEARNING_GOALS = "Learning goals coming soon";

    public Simulation() {
    }

    public String getDescriptionKey() {
        return "simulation." + name + ".description";
    }

    public String getLearningGoalsKey() {
        return "simulation." + name + ".learningGoals";
    }

    /**
     * @param session - hibernate session
     * @return The HTML version of the given sim or null
     */
    public Simulation getHTMLVersion( Session session ) {
        String simulationName = getName();

        // try looking up the sim from the mapping in case it is not found due to a name mismatch between the legacy and html versions
        if ( AbstractSimulationPage.LEGACY_TO_CURRENT_SIM_NAME.containsKey( simulationName ) ) {
            simulationName = AbstractSimulationPage.LEGACY_TO_CURRENT_SIM_NAME.get( simulationName );
        }

        List<Simulation> sims = session.createQuery( "select s from Simulation as s where s.name = :name" ).setString( "name", simulationName ).list();
        for ( Simulation s : sims ) {
            if ( s.isVisible() && s.isHTML() ) {
                return s;
            }
        }
        return null;
    }

    /**
     * @param session - hibernate session
     * @return The legacy version of the given sim or null
     */
    public Simulation getLegacyVersion( Session session ) {
        String simulationName = getName();

        // try looking up the sim from the mapping in case it is not found due to a name mismatch between the legacy and html versions
        if ( AbstractSimulationPage.CURRENT_SIM_NAME_TO_LEGACY.containsKey( simulationName ) ) {
            simulationName = AbstractSimulationPage.CURRENT_SIM_NAME_TO_LEGACY.get( simulationName );
        }

        List<Simulation> sims = session.createQuery( "select s from Simulation as s where s.name = :name" ).setString( "name", simulationName ).list();
        for ( Simulation s : sims ) {
            if ( s.isVisible() && !s.isHTML() ) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the best matching localized simulation.
     * <p/>
     * Note: should be in a Hibernate session, so localizedSimulations is instantiated
     *
     * @param bestLocale
     * @return
     */
    public LocalizedSimulation getBestLocalizedSimulation( Locale bestLocale ) {
        LocalizedSimulation englishSimulation = null;
        LocalizedSimulation languageSimulation = null;
        Locale englishLocale = LocaleUtils.stringToLocale( "en" );
        Locale languageLocale = LocaleUtils.stringToLocale( bestLocale.getLanguage() );
        for ( Object localizedSimulation : localizedSimulations ) {
            LocalizedSimulation sim = (LocalizedSimulation) localizedSimulation;
            if ( sim.getLocale().equals( bestLocale ) ) {
                return sim;
            }
            else if ( sim.getLocale().equals( englishLocale ) ) {
                englishSimulation = sim;
            }
            else if ( sim.getLocale().equals( languageLocale ) ) {
                languageSimulation = sim;
            }
        }
        return languageSimulation != null ? languageSimulation : englishSimulation;
    }

    public LocalizedSimulation getEnglishSimulation() {
        return getBestLocalizedSimulation( LocaleUtils.stringToLocale( "en" ) );
    }

    /**
     * @return A relative URL for the thumbnail
     */
    public String getThumbnailUrl() {
        // NOTE: this is relied upon to be relative!
        if ( isHTML() ) {
            return "/sims/" + getProject().getName() + "/latest/" + getName() + "-128.png";
        }

        // improved-quality PNG version of the thumbnail
        String pngUrl = "/sims/" + getProject().getName() + "/" + getName() + "-thumbnail.png";

        // old JPEG low-quality thumbnail. TODO: (performance) remove this and the below check on file existence AFTER ALL .png thumbnails are in place
        String jpgUrl = "/sims/" + getProject().getName() + "/" + getName() + "-thumbnail.jpg";

        String newThumbnailUrl = "/sims/" + getProject().getName() + "/" + getName() + "-128.png";

        if ( UrlUtils.getDocrootFile( newThumbnailUrl ).exists() ) {
            return newThumbnailUrl;
        }
        else if ( UrlUtils.getDocrootFile( pngUrl ).exists() ) {
            return pngUrl;
        }
        else {
            return jpgUrl;
        }
    }

    public String getImageUrl() {
        if ( isHTML() ) {
            return "/sims/" + getProject().getName() + "/latest/" + getName() + "-600.png";
        }
        else {
            String newScreenshotUrl = "/sims/" + getProject().getName() + "/" + getName() + "-600.png";
            if ( UrlUtils.getDocrootFile( newScreenshotUrl ).exists() ) {
                return newScreenshotUrl;
            }
            else {
                return "/sims/" + getProject().getName() + "/" + getName() + "-screenshot.png";
            }
        }
    }

    public WebImage getThumbnail() {
        return WebImage.get( getThumbnailUrl(), false );
    }

    public WebImage getImage() {
        return WebImage.get( getImageUrl(), false );
    }

    public String getScreenshotURL() {
        return "/sims/" + getProject().getName() + "/" + project.getVersionString() + "/" + getName() + "-600.png";
    }

    public WebImage getHTMLImage() {
        return WebImage.get( getScreenshotURL(), false );
    }

    public WebImage getHTMLThumbnail() {
        return WebImage.get( "/sims/" + getProject().getName() + "/" + project.getVersionString() + "/" + getName() + "-128.png", false );
    }

    public int detectSimKilobytes( File docRoot ) {
        File projectRoot = project.getProjectRoot( docRoot );
        switch( getType() ) {
            case Project.TYPE_JAVA:
                return (int) ( new File( projectRoot, project.getName() + "_all.jar" ) ).length() / 1000;
            case Project.TYPE_FLASH:
                return (int) ( new File( projectRoot, name + "_en.jar" ) ).length() / 1024;
            case Project.TYPE_HTML:
                return (int) ( new File( Project.getLatestHTMLDirectory( projectRoot ), name + "_en.html" ).length() / 1024 );
            default:
                throw new RuntimeException( "Simulation type not handled? type = " + getType() );
        }
    }

    public static File getAllJar( File docRoot, String project ) {
        return new File( docRoot, "sims/" + project + "/" + project + "_all.jar" );
    }

    public static File getLocalizedJar( File docRoot, String project, String sim, Locale locale ) {
        return new File( docRoot, "sims/" + project + "/" + sim + "_" + LocaleUtils.localeToString( locale ) + ".jar" );
    }

    public boolean isJava() {
        return getType() == 0;
    }

    public boolean isFlash() {
        return getType() == 1;
    }

    public boolean isHTML() {
        return getType() == 2;
    }

    public GradeLevel getMinGradeLevel() {
        boolean foundGradeLevelCategory = false;
        GradeLevel result = GradeLevel.getHighestGradeLevel();
        for ( Object o : categories ) {
            Category category = (Category) o;
            if ( category.isGradeLevelCategory() ) {
                foundGradeLevelCategory = true;
                GradeLevel level = GradeLevel.getGradeLevelFromCategory( category );
                if ( GradeLevel.isLowerGradeLevel( level, result ) ) {
                    result = level;
                }
            }
        }

        // if we have no grade level information, include everything
        if ( !foundGradeLevelCategory ) {
            return GradeLevel.getLowestGradeLevel();
        }
        return result;
    }

    public GradeLevel getMaxGradeLevel() {
        boolean foundGradeLevelCategory = false;
        GradeLevel result = GradeLevel.getLowestGradeLevel();
        for ( Object o : categories ) {
            Category category = (Category) o;
            if ( category.isGradeLevelCategory() ) {
                foundGradeLevelCategory = true;
                GradeLevel level = GradeLevel.getGradeLevelFromCategory( category );
                if ( GradeLevel.isLowerGradeLevel( result, level ) ) {
                    result = level;
                }
            }
        }

        // if we have no grade level information, include everything
        if ( !foundGradeLevelCategory ) {
            return GradeLevel.getHighestGradeLevel();
        }
        return result;
    }

    /**
     * @return LRETerm instances that refer to related LRE-0001 terms that relate to this simulation
     */
    public Set<LRETerm> getLRETerms() {
        // need to pull this from the stored IDs
        Set ids = getLreTermIDs();
        Set<LRETerm> result = new HashSet<LRETerm>();
        for ( Object o : ids ) {
            result.add( LRETerm.getTermFromId( (String) o ) );
        }
        return result;
    }

    public void addLRETerm( LRETerm term ) {
        getLreTermIDs().add( term.id );
    }

    public void removeLRETerm( LRETerm term ) {
        getLreTermIDs().remove( term.id );
    }

    // getters and setters

    public int getId() {
        return id;
    }

    public void setId( int id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public int getType() {
        return project.getType();
    }

    public Project getProject() {
        return project;
    }

    public void setProject( Project project ) {
        this.project = project;
    }

    public Set getLocalizedSimulations() {
        return localizedSimulations;
    }

    public void setLocalizedSimulations( Set localizedSimulations ) {
        this.localizedSimulations = localizedSimulations;
    }

    public Set getCategories() {
        return categories;
    }

    public void setCategories( Set categories ) {
        this.categories = categories;
    }

    public Set getAlignments() {
        return alignments;
    }

    public void setAlignments( Set alignments ) {
        this.alignments = alignments;
    }

    public Set getSecondaryAlignments() {
        return secondaryAlignments;
    }

    public void setSecondaryAlignments( Set secondaryAlignments ) {
        this.secondaryAlignments = secondaryAlignments;
    }

    public Set getScienceLiteracyMapKeys() {
        return scienceLiteracyMapKeys;
    }

    public void setScienceLiteracyMapKeys( Set scienceLiteracyMapKeys ) {
        this.scienceLiteracyMapKeys = scienceLiteracyMapKeys;
    }

    public Set getLreTermIDs() {
        return lreTermIDs;
    }

    public void setLreTermIDs( Set lreTermIDs ) {
        this.lreTermIDs = lreTermIDs;
    }

    public List getKeywords() {
        return keywords;
    }

    public void setKeywords( List keywords ) {
        this.keywords = keywords;
    }

    public int getKilobytes() {
        return kilobytes;
    }

    public void setKilobytes( int kilobytes ) {
        this.kilobytes = kilobytes;
    }

    public List getTopics() {
        return topics;
    }

    public void setTopics( List topics ) {
        this.topics = topics;
    }

    public List getRelatedSimulations() {
        return relatedSimulations;
    }

    public void setRelatedSimulations( List relatedSimulations ) {
        this.relatedSimulations = relatedSimulations;
    }

    public String getDesignTeam() {
        return designTeam;
    }

    public void setDesignTeam( String designTeam ) {
        this.designTeam = designTeam;
    }

    public String getLibraries() {
        return libraries;
    }

    public void setLibraries( String libraries ) {
        this.libraries = libraries;
    }

    public String getThanksTo() {
        return thanksTo;
    }

    public void setThanksTo( String thanksTo ) {
        this.thanksTo = thanksTo;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl( String videoUrl ) {
        this.videoUrl = videoUrl;
    }

    public boolean isUnderConstruction() {
        return underConstruction;
    }

    public void setUnderConstruction( boolean underConstruction ) {
        this.underConstruction = underConstruction;
    }

    public boolean isGuidanceRecommended() {
        return guidanceRecommended;
    }

    public void setGuidanceRecommended( boolean guidanceRecommended ) {
        this.guidanceRecommended = guidanceRecommended;
    }

    public boolean isClassroomTested() {
        return classroomTested;
    }

    public void setClassroomTested( boolean classroomTested ) {
        this.classroomTested = classroomTested;
    }

    public boolean isSimulationVisible() {
        return simulationVisible;
    }

    public void setSimulationVisible( boolean simulationVisible ) {
        this.simulationVisible = simulationVisible;
    }

    public boolean isVisible() {
        return isSimulationVisible() && project.isVisible();
    }

    /**
     * @return Whether this simulation is covered by the CC-BY 3.0 license. See Linkers.CC_BY_3
     */
    public boolean isHasCreativeCommonsAttributionLicense() {
        return hasCreativeCommonsAttributionLicense;
    }

    public void setHasCreativeCommonsAttributionLicense( boolean hasCreativeCommonsAttributionLicense ) {
        this.hasCreativeCommonsAttributionLicense = hasCreativeCommonsAttributionLicense;
    }

    public boolean isHasGPL2License() {
        // currently all sims are licensed under GPL 2.0
        return true;
    }

    /**
     * @return List of license URLs that this simulation is covered by
     */
    public List<String> getLicenseURLs() {
        List<String> result = new ArrayList<String>();
        if ( isHasCreativeCommonsAttributionLicense() ) {
            result.add( Licenses.CC_BY_3 );
        }
        if ( isHasGPL2License() ) {
            result.add( Licenses.CC_GPL_2 );
        }
        return result;
    }

    public Set getContributions() {
        return contributions;
    }

    public void setContributions( Set contributions ) {
        this.contributions = contributions;
    }

    public int getOldId() {
        return oldId;
    }

    public void setOldId( int oldId ) {
        this.oldId = oldId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime( Date createTime ) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime( Date updateTime ) {
        this.updateTime = updateTime;
    }

    public GradeLevel getLowGradeLevel() {
        return lowGradeLevel == null ? GradeLevel.getLowestGradeLevel() : lowGradeLevel;
    }

    public void setLowGradeLevel( GradeLevel lowGradeLevel ) {
        this.lowGradeLevel = lowGradeLevel;
    }

    public GradeLevel getHighGradeLevel() {
        return highGradeLevel == null ? GradeLevel.getHighestGradeLevel() : highGradeLevel;
    }

    public void setHighGradeLevel( GradeLevel highGradeLevel ) {
        this.highGradeLevel = highGradeLevel;
    }

    public FAQList getFaqList() {
        return faqList;
    }

    public void setFaqList( FAQList faqList ) {
        this.faqList = faqList;
    }

    public boolean isFaqVisible() {
        return faqVisible;
    }

    public void setFaqVisible( boolean faqVisible ) {
        this.faqVisible = faqVisible;
    }
}
