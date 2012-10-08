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
package org.ucd.osmwikicrawler

import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.ucd.osmwikicrawler.crawler.Crawler
import org.ucd.osmwikicrawler.ontology.Lgd
import org.ucd.osmwikicrawler.ontology.OsmOntology
import org.ucd.osmwikicrawler.rdf.*
import org.ucd.osmwikicrawler.utils.DumpUtils;
import org.ucd.osmwikicrawler.utils.Utils

/**
 * 
 * @author Andrea Ballatore
 * @param args
 */
class RunMappingOsnToLod {
	
	static def log = Logger.getLogger(RunMappingOsnToLod)
	
	private static void mapOsnWithLodDatasets( boolean wordnet = false, boolean dbpedia = false ){
		// launch mapping process
		
	}
	
	public static void main(String[] args) {
		// init logger
		PropertyConfigurator.configure( "conf/log4j.properties" );
		log.info "\n**** OsmWikiCrawler ****\n"
		
		
		
		
		log.info "*** Mapping the OSM Semantic Network to other datasets ***"
		
				
		log.info "\n**** LOD mapping finished. Check output folders. ****\n"
	}
}