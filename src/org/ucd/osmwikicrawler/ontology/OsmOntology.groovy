/*
This file is part of the OSM Wiki Crawler.

   The OSM Wiki Crawler is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   The OSM Wiki Crawler is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the OSM Wiki Crawler.  If not, see <http://www.gnu.org/licenses/>.
   
   Author: Andrea Ballatore
   Project home page: http://gforge.ucd.ie/projects/osm-similarity/
*/
package org.ucd.osmwikicrawler.ontology

import java.util.Set;

/**
 * 
 * @author Andrea Ballatore
 *
 */
class OsmOntology {
	
	Date creationDate = new Date()
	
	Long nTermsCount = -1
	Long nTermsValid = -1
	Long nTermsFailed = -1
	Long termsWithWpLinks = -1
	Long lgdClassCount = -1
	Long lgdClassFound = -1
	Long osmKeyCount = -1
	Long osmValueCount = -1
	Long redirectionCount = -1
	
	Long mergedTerms = 0
	Long wikiTableFound = 0
	Long termsBuiltFromWikiTables = 0
	
	Set terms = []//OsmOntoTerm
	Set failedTerms = []// OsmOntoTerm
}
