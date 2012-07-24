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
import info.bliki.api.creator.*
import info.bliki.wiki.*
import info.bliki.wiki.dump.*
import java.util.SortedSet
import org.apache.log4j.Logger
import javax.xml.parsers.SAXParserFactory

import org.ucd.osmwikicrawler.crawler.*
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.*
import info.bliki.wiki.filter.*
import info.bliki.wiki.impl.*
import org.xml.sax.SAXException;
import java.io.IOException;


/**
 * Utils for generating HTML code from XML dump obtained from http://dump.wiki.openstreetmap.org
 * This code is heavily based on http://code.google.com/p/gwtwiki/wiki/MediaWikiDumpSupport
 *
 * @author Andrea Ballatore
 */
public class WikiDumpToHtml {
	
	static def log = Logger.getLogger(WikiDumpToHtml.class)
	
    public WikiDumpToHtml() {
         super();
    }
	
	static String _COMPLETED_FILE_NAME = "TASK_COMPLETED"
	static String _HTML_HEADER = '''<!DOCTYPE html>
		<html lang="en" dir="ltr">
		<head>
			<meta charset="utf-8" />
			<title></title>
		</head>
		<body class="mediawiki ltr ns-0 ns-subject skin-vector">'''
	static String _HTML_FOOTER = '''</body>
		</html>'''
	
    static class DemoArticleFilter implements IArticleFilter {
        WikiDB wikiDB;
        int counter;
        private final String htmlDirectory;
        private final String imageDirectory;

        public DemoArticleFilter(WikiDB db, String htmlDirectory, String imageDirectory) {
            this.counter = 0;
            this.wikiDB = db;
            if (htmlDirectory.charAt(htmlDirectory.length() - 1) != '/') {
                htmlDirectory = htmlDirectory + "/";
            }
            this.htmlDirectory = htmlDirectory;
            this.imageDirectory = imageDirectory;
        }
		
		
		/**
		 * Process entry
		 */
        public void process( WikiArticle page, Siteinfo siteinfo ) throws SAXException {
            if (page.isMain() || page.isCategory() || page.isProject()) {
				String title = page.getTitle();
                String titleURL = getLocalFileNameForPage( Encoder.encodeTitleLocalUrl( title ) );
				log.debug("Processing page: " + title + " > " + titleURL)
                
				String generatedHTMLFilename = htmlDirectory + titleURL; // + ".html";
				DumpWikiModel wikiModel = new DumpWikiModel(wikiDB, siteinfo, "\${image}", "\${title}", imageDirectory);
				DumpDocumentCreator creator = new DumpDocumentCreator(wikiModel, page);
				
				creator.setHeader( _HTML_HEADER );
				creator.setFooter( _HTML_FOOTER );
                wikiModel.setUp();
                try {
                    creator.renderToFile(generatedHTMLFilename);
                    System.out.print('.');
                    if (++counter >= 80) {
                         System.out.println(' ');
                         counter = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e1) {
					e1.printStackTrace();
                }
            }
        }
    }

    static class DemoTemplateArticleFilter implements IArticleFilter {
        WikiDB wikiDB;
        int counter;

        public DemoTemplateArticleFilter(WikiDB wikiDB) {
                this.wikiDB = wikiDB;
                this.counter = 0;
        }

        public void process(WikiArticle page, Siteinfo siteinfo) throws SAXException {
            if (page.isTemplate()) {
                // System.out.println(page.getTitle());
                TopicData topicData = new TopicData(page.getTitle(), page.getText());
                try {
                    wikiDB.insertTopic(topicData);
                    System.out.print('.');
                    if (++counter >= 80) {
                            System.out.println(' ');
                            counter = 0;
                    }
                } catch (Exception e) {
                    String mess = e.getMessage();
                    if (mess == null) {
                            throw new SAXException(e.getClass().getName());
                    }
                    throw new SAXException(mess);
                }
            }
		}
    }

    public static WikiDB prepareDB(String mainDirectory) {
            // the following subdirectory should not exist if you would like to create a
            // new database
            if (mainDirectory.charAt(mainDirectory.length() - 1) != '/') {
                mainDirectory = mainDirectory + "/";
            }
            String databaseSubdirectory = "WikiDumpDB";

            WikiDB db = null;

            try {
                db = new WikiDB(mainDirectory, databaseSubdirectory);
                return db;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                // if (db != null) {
                // try {
                // db.tearDown();
                // } catch (Exception e) {
                // e.printStackTrace();
                // }
                // }
            }
            return null;
    }

	/**
	 * 
	 * @param xmlFile
	 */
    public static void genHtmlFromXmlDump( String xmlFile, boolean skipFirstPass = false ) {
        // String bz2Filename = "c:\\temp\\dewikiversity-20100401-pages-articles.xml.bz2";
		
		
        String bz2Filename = xmlFile;
        WikiDB db = null;
		final String flagFile = Utils.getOutputFolder() + File.separator + _COMPLETED_FILE_NAME
		
        try {
			//new File( flagFile ).delete()
			
            String mainDirectory = Utils.getOutputFolder() + "/wiki_dump";
            String htmlDirectory = Utils.getPageCacheFolder();
			

            // the following directory must exist for image references
            String imageDirectory = "dump/WikiDumpImages";
            log.info("Prepare wiki database");
            db = prepareDB(mainDirectory);
            IArticleFilter handler;
            WikiXMLParser wxp;
            if (!skipFirstPass) {
                log.info("First pass - write templates to database:");
                handler = new DemoTemplateArticleFilter(db);
                wxp = new WikiXMLParser(bz2Filename, handler);
                wxp.parse();
                System.out.println(' ');
            }
            log.info("Second pass - write HTML files to directory in folder $htmlDirectory");
            handler = new DemoArticleFilter(db, htmlDirectory, imageDirectory);
            wxp = new WikiXMLParser(bz2Filename, handler);
            wxp.parse();
            System.out.println(' ');
            System.out.println("Done!");
        } catch (Exception e) {
                e.printStackTrace();
        } finally {
            if (db != null) {
                try {
                    db.tearDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
	
	
	/**
	 *
	 * @param wiki page title, e.g. Tag:highway=bus_guideway
	 * @return e.g. http%3A%2F%2Fwiki.openstreetmap.org%2Fwiki%2FTag%3Ahighway%253Dbus_guideway
	 */
	public static String getLocalFileNameForPage( String title ){
		assert title
		title = title.replaceAll("'","%")
		title = title.replaceAll("/","%2F")
		title = title.replaceAll('\\(',"%28")
		title = title.replaceAll('\\)',"%29")
		String outTitle = URLEncoder.encode( Crawler.OSM_WIKI_BASE_URL ) + title
		int _MAX_LENGTH = 255
		if (outTitle.length() > _MAX_LENGTH ){
			// cut string 
			outTitle = outTitle[0.._MAX_LENGTH-1]
		}
		assert (outTitle.length() <= _MAX_LENGTH)
		return outTitle
	}
}


