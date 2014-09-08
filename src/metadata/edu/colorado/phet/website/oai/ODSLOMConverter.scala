// Copyright 2002-2013, University of Colorado
package edu.colorado.phet.website.oai

import org.dlese.dpc.xml.XMLFormatConverter

/**
 * Stub format class that allows us to not include the DLESE (very large) code as a dependency for the website.
 */
class ODSLOMConverter extends XMLFormatConverter with edu.colorado.phet.website.metadata.formats.ODSLOMConverter
