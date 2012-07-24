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
package org.ucd.osmwikicrawler.utils

import java.awt.Color
import info.bliki.wiki.*
import info.bliki.wiki.dump.*
import java.io.*
import java.util.SortedSet
import org.apache.log4j.Logger
import javax.xml.parsers.SAXParserFactory

import org.ucd.osmwikicrawler.crawler.Crawler;
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import org.apache.derby.jdbc.*

/**
 * Utils for manipulating XML dump from
 * http://dump.wiki.openstreetmap.org
 * 
 * @author Andrea Ballatore
 *
 */
class DumpUtils {
	
	static def log = Logger.getLogger(DumpUtils.class)
	
	/** XML Dump locations */
	static final String WIKI_DUMP_ROOT_URL = "http://dump.wiki.openstreetmap.org/"
	static final String LATEST_DUMP_XML_URL = WIKI_DUMP_ROOT_URL + "osmwiki-latest-current.xml"
	
	/**
	 * Download and uncompress XML Wiki Dump file.
	 * 
	 * @return File path to unzipped XML dump
	 */
	private static String downloadFullXmlDump(){
		String xmlContent = ''
		Crawler.createFolders()
		String content = null
		String uri = LATEST_DUMP_XML_URL
		String uriGz = LATEST_DUMP_XML_URL + ".gz"
		String fn = Utils.getDumpFileName( uri, Utils.getTempFolder() )
		String fnGz = Utils.getDumpFileName( uriGz, Utils.getTempFolder() )
		assert fnGz
		if (!Utils.isCachedPageStillValid(fnGz)){
			// download DUMP.xml.gz
			log.info("   Downloading dump file $uriGz (this may take several minutes)...")
			boolean ok = Utils.downloadURLBinary( uriGz, fnGz )
			assert ok
			//Utils.outputFile( Utils.getWebPageByURI( uriGz, true )?.content, fnGz )  
			log.info("   Dump file downloaded.")
		}
		if (!Utils.isCachedPageStillValid(fn)){
			log.info("   Uncompressing archive...")
			boolean extracted = Utils.gunzipFile( fnGz, fn )
			assert extracted
		}
		log.info("   XML Dump ready.")
		return fn
	}

	
	/**
	 * Download Wiki dump, and generate HTML pages from it.
	 * 
	 * Based on 
	 * http://code.google.com/p/gwtwiki/source/browse/trunk/info.bliki.wiki/bliki-core/src/test/java/info/bliki/wiki/dump/DumpExample.java
	 * 
	 * @return not used
	 */
	static public int setupWikiDump(){
		String xmlFilePath = downloadFullXmlDump()
		log.info("Analysing XML dump...")
		String inF = xmlFilePath + ".gz"
		WikiDumpToHtml.genHtmlFromXmlDump( inF )
		log.info("...Done.")
		
		return 0
	}
	
	/**
	 * 
	 */
	static class WikiArticleFilter implements IArticleFilter {
		public boolean process(WikiArticle page) {
				System.out.println("----------------------------------------");
				System.out.println(page.getTitle());
				System.out.println("----------------------------------------");
				System.out.println(page.getText());
				return true;
		}
		public void process(WikiArticle page, Siteinfo info ) {
			process(page)
		}

	}

	
	/**
	 * Tool to read XML dump
	 *
	 */
	static class XmlWikiDumpHandler extends DefaultHandler {
	    def messages = []
	    def currentMessage
	    def countryFlag = false
		
	    void startElement(String ns, String localName, String qName, Attributes atts) {
			log.info("called here")
	        switch (qName) {
	           case 'car':
	               currentMessage = atts.getValue('make') + ' of '; break
	           case 'country':
	               countryFlag = true; break
	           case 'record':
	               currentMessage += atts.getValue('type') + ' record'; break
	        }
	    }
		
	    void characters(char[] chars, int offset, int length) {
			log.info("called here")
			if (countryFlag) {
				currentMessage += new String(chars, offset, length)
			}
	    }
		
	    void endElement(String ns, String localName, String qName) {
	        switch (qName) {
	           case 'car':
	               messages << currentMessage; break
	           case 'country':
	               currentMessage += ' has a '; countryFlag = false; break
	        }
	    }
	}

}


