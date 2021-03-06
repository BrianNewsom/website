package edu.colorado.phet.website.metadata.formats

import xml.Node
import edu.colorado.phet.website.constants.Licenses
import edu.colorado.phet.website.metadata.MetadataUtils._
import edu.colorado.phet.website.metadata.{MetadataUtils, SimulationRecord, PhetMetadataConverter}

/**
 * Converts our master format simulation data to IEEE LOM
 *
 * metadata format spec: http://ltsc.ieee.org/wg12/files/LOM_1484_12_1_v1_Final_Draft.pdf
 * XML binding spec: http://ltsc.ieee.org/wg12/files/IEEE_1484_12_03_d8_submitted.pdf
 *
 * NOTE: do not change the package / class name of this, since it is referenced in the server-side web.xml of the
 * jOAI webapp
 */
trait IEEELOMConverter extends PhetMetadataConverter {
  def getToFormat = "lom"

  override def toString = getToFormat

  def getSchemaURI = Some("http://ltsc.ieee.org/xsd/LOM")

  def getLRSchemaTypes = List("IEEE LOM 2002")

  def convertRecord(record: SimulationRecord): Node = {

    /*
    Physics classification example (LRE-0001)
                <classification>
                    <purpose>
                        <source>LOMv1.0</source>
                        <value>discipline</value>
                    </purpose>
                    <taxonPath>
                        <source>
                            <string language="x-none">LRE-0001</string>
                        </source>
                        <taxon>
                            <id>978</id>
                            <entry>
                                <string language="en">physics</string>
                            </entry>
                        </taxon>
                    </taxonPath>
                </classification>


  <classification>
    <purpose>
      <source>LOMv1.0</source>
      <value>discipline</value>
    </purpose>
    <taxonPath>
      <source>
        <string language="none">learndirect</string>
      </source>
      <taxon>
        <id>FC.4</id>
        <entry>
          <string language="none">English Literature</string>
        </entry>
      </taxon>
    </taxonPath>
    <taxonPath>
      <source>
        <string language="none">JACS</string>
      </source>
      <taxon>
        <id>Q320</id>
        <entry>
          <string language="none">English Literature</string>
        </entry>
      </taxon>
    </taxonPath>
  </classification>

     */

    <lom xmlns="http://ltsc.ieee.org/xsd/LOM"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://ltsc.ieee.org/xsd/LOM http://ltsc.ieee.org/xsd/lomv1.0/lomLoose.xsd">
      <!-- LOM 1 General -->
      <general>
        <!-- identifier -->
        <identifier>
          <catalog>phet.colorado.edu</catalog>
          <entry>
            {record.simPageLink}
          </entry>
        </identifier>

        <!-- title -->
        <title>
          {convertLangString(record.translatedTitles)}
        </title>

        <!-- languages -->{record.languages.map(language => <language>
        {language}
      </language>)}<!-- description -->
        <description>
          {convertLangString(record.translatedDescriptions)}
        </description>

        <!-- keywords -->{record.translatedTerms.map(term => <keyword>
        {convertLangString(term)}
      </keyword>)}<structure>
        <source>LOMv1.0</source>
        <value>atomic</value>
      </structure>
        <aggregationLevel>
          <source>LOMv1.0</source>
          <value>1</value>
        </aggregationLevel>
      </general>

      <!-- LOM 2 Lifecycle -->
      <lifeCycle>

        <!-- simulation version -->
        <version>
          <string language="en">
            {record.versionString}
          </string>
        </version>

        <status>
          <source>LOMv1.0</source>
          <value>final</value>
        </status>

        <!-- PhET -->
        <contribute>
          <role>
            <source>LOMv1.0</source>
            <value>publisher</value>
          </role>
          <entity>
            <![CDATA[BEGIN:VCARD
N:;;PhET Interactive Simulations
FN:PhET Interactive Simulations
ORG:PhET Interactive Simulations
VERSION:3.0
END:VCARD]]>

          </entity>
        </contribute>

        <!-- other contributors -->{record.authors.map(author => <contribute>
        <role>
          <source>LOMv1.0</source>
          <value>author</value>
        </role>
        <entity>
          {MetadataUtils.vCardFromName(author)}
        </entity>
      </contribute>)}
      </lifeCycle>

      <!-- TODO consider LOM 3 Meta-Metadata -->

      <!-- LOM 4 Technical -->
      <technical>
        {record.mimeTypes.map(mimeType => <format>
        {mimeType}
      </format>)}<size>
        {( record.kilobytes * 1000 ).toString}
      </size>
        <location>
          {record.simPageLink}
        </location>
        <installationRemarks>
          <string language="en">Press either "Run Now!" to run the simulation, or "Download" to download it to your computer to run later</string>
        </installationRemarks>
        <otherPlatformRequirements>
          <string language="en">
            {record.englishSoftwareRequirements}
          </string>
        </otherPlatformRequirements>
      </technical>

      <!-- LOM 5 Educational -->
      <educational>
        <!-- interactivity type -->
        <interactivityType>
          <source>LOMv1.0</source> <value>active</value>
        </interactivityType>

        <!-- learning resource type -->
        <learningResourceType>
          <source>LOMv1.0</source> <value>simulation</value>
        </learningResourceType>
        <learningResourceType>
          <source>DCMIType</source> <value>InteractiveResource</value>
        </learningResourceType>

        <!-- interactivity level -->
        <interactivityLevel>
          <source>LOMv1.0</source> <value>very high</value>
        </interactivityLevel>

        <!-- semantic density -->
        <semanticDensity>
          <source>LOMv1.0</source> <value>very high</value>
        </semanticDensity>

        <!-- intended user roles -->
        <intendedEndUserRole>
          <source>LOMv1.0</source> <value>teacher</value>
        </intendedEndUserRole>
        <intendedEndUserRole>
          <source>LOMv1.0</source> <value>learner</value>
        </intendedEndUserRole>

        <!-- contexts -->
        <context>
          <source>LOMv1.0</source> <value>school</value>
        </context>
        <context>
          <source>LOMv1.0</source> <value>higher education</value>
        </context>

        <typicalAgeRange>
          <string language="x-t-lre">
            {record.minGradeLevel.getLowAge + "-" + record.maxGradeLevel.getHighAge}
          </string>
        </typicalAgeRange>

        <!-- OPTION difficulty: very easy / easy / medium / difficult / very difficult -->
        <!--<difficulty><source>LOMv1.0</source><value>medium</value></difficulty>-->

        <!-- languages (again, see above in LOM 1 General) -->{record.languages.map(language => <language>
        {language}
      </language>)}
      </educational>

      <!-- LOM 6 Rights -->
      <rights>
        <cost>
          <source>LOMv1.0</source> <value>no</value>
        </cost>

        <copyrightAndOtherRestrictions>
          <source>LOMv1.0</source> <value>yes</value>
        </copyrightAndOtherRestrictions>
        <description>
          {record.licenseURLs.map(licenseURL => licenseURL match {
          case Licenses.CC_BY_3 => <string language="x-t-cc-url">
            {Licenses.CC_BY_3}
          </string>
          case Licenses.CC_GPL_2 => <string language="x-t-rights-url">
            {Licenses.CC_GPL_2}
          </string>
        })}{convertLangString(record.rights)}
        </description>
      </rights>

      <!-- LOM 7 Relation -->
      <relation>
        <!-- TODO: LOM 7 Relation related to other sims, or their teacher guides? -->
      </relation>

      <!-- TODO: LOM 8 annotations? (like other people's comments in our metadata) -->

      <!-- LRE 9 Classification -->
      <!-- LRE terms -->{record.lreTerms.map(term => <classification>
      <purpose>
        <source>LOMv1.0</source>
        <value>discipline</value>
      </purpose>
      <taxonPath>
        <source>
          <string language="x-none">LRE-0001</string>
        </source>
        <taxon>
          <id>
            {term._1}
          </id>
          <entry>
            <string language="en">
              {term._2}
            </string>
          </entry>
        </taxon>
      </taxonPath>
    </classification>)}
    </lom>
  }
}