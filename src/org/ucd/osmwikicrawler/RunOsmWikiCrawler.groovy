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
class RunOsmWikiCrawler {
	
	static def log = Logger.getLogger(RunOsmWikiCrawler)
	
	/**
	 * 
	 * @param lgd include mapping to LinkedGeoData 
	 */
	private static void crawlOsmWiki( boolean lgd = false ){
		// launch crawler
		Crawler crawler = new Crawler()
		// main method, might take long time
		OsmOntology onto = crawler.createOntologyFromOsmWiki()
		if (lgd){
			// map LGD classes
			try {
				onto = Lgd.matchOsmOntoTermsWithLgd( onto )
			} catch(e){
				log.warn "Failed to map terms on LinkedGeoData: $e"
			}
		}
		// generate NT output file
		WikiRdf.genRdfFiles( onto )
		def msg = Utils.LOG_ROW + "RDF files generated in folder: ${Utils.getSemantiNetworkOutputFolder()}" + Utils.LOG_ROW
		log.info msg
	}
	
	/**
	 * Run the crawler.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// init logger
		PropertyConfigurator.configure( "conf/log4j.properties" );
		log.info "\n**** OsmWikiCrawler ****\n"
		
		// read parameters: TODO: insert in properties file or take from args
		boolean findLgdMapping = true
		boolean generateHtmlFromDump = true
		
		// download and extract pages from XML dump
		if (generateHtmlFromDump){
			DumpUtils.setupWikiDump()
		}
		
		// generate OSM Semantic network
		log.info "\n**** Extract OSM Semantic Network ****\n"
		crawlOsmWiki( findLgdMapping )
		
		log.info "\n**** OsmWikiCrawler has finished. Check output folders. ****\n"
	}
}