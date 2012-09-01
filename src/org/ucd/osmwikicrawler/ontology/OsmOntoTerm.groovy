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

/**
 * Meta class aggregating all info about a OSM Tag.
 * 
 * 
 * @author andreaballtore
 *
 */
class OsmOntoTerm {
	
	String key 
	String value
	String multiValues = null
	String uri
	
	Boolean bPseudoUri = false
	
	Boolean bNode = false
	Boolean bWay = false
	Boolean bArea = false
	Boolean bRelation = false
	
	String redirectionUri = null
	
	Boolean foundInOverviewPage = false
	Boolean foundInSinglePage = false
	Boolean foundInProposedPage = false
	
	/** WIKI info */
	String wikiKeyUris = ''
	// description in Tag individual page
	String description = ''
	String descriptionUris = ''
	String impliesUris = ''
	String combinationUris = ''
	String renderingUri = ''
	String photoUris = ''
	String sourceUri = ''
	
	String tagCommunityStatus = ''
	
	/** LGD */
	String lgdUri = ''
	
	/** DBP */
	String dbpOntoUri = ''
	String dbpResUri = ''
	String dbpSkosUri = ''
	
	/** STATS */
	Long lgdWayCount = -1
	Long lgdNodeCount = -1
	Long lgdTotalCount = -1
	
	Long osmWayCount = -1
	Long osmNodeCount = -1
	Long osmRelCount = -1
	Long osmTotalCount = -1
	
	Boolean bFailedToBuild = false
	String sFailedToBuild = ''
	
	OsmOntology ontology = null
	
	String toString(){
		String s = "{ OsmOntoTerm key='${key}' value='${value}' multiValues='${multiValues}' uri=$uri bArea=${bArea} bNode=${bNode} bWay=${bWay} bRelation=$bRelation keyUris='${wikiKeyUris}' "
		s += "renderingUri='${renderingUri}' photoUris='${photoUris}' description='$description' descriptionUris='$descriptionUris' impliesUris='$impliesUris' combinationUris='$combinationUris' "
		s += "lgdWayCount=${lgdWayCount} lgdNodeCount=${lgdNodeCount} lgdTotalCount=${lgdTotalCount} "
		s += "osmWayCount=${osmWayCount} osmRelCount=${osmRelCount} osmNodeCount=${osmNodeCount} osmTotalCount=${osmTotalCount} "
		s += "foundInOverviewPage=${foundInOverviewPage} foundInSinglePage=${foundInSinglePage} foundInProposedPage=$foundInProposedPage tagCommunityStatus=$tagCommunityStatus "
		if (bFailedToBuild){
			s += "sFailedToBuild=$sFailedToBuild"
		}
		s += " redirectionUri=$redirectionUri }"
		return s
	}
}
