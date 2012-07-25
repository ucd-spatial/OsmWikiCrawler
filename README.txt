=====================================================================
	OpenStreetMap Wiki Crawler
=====================================================================

The OpenStreetMap Wiki Crawler is an open source tool that extracts
the OpenStreetMap Semantic Network from the OpenStreetMap Wiki website 
(http://wiki.openstreetmap.org).
Pre-extracted RDF graphs are available at
http://github.com/ucd-spatial/OsmSemanticNetwork


---------------------------------------------------------------------
	Author and Contact
---------------------------------------------------------------------
Author: Andrea Ballatore (University College Dublin)
		http://sites.google.com/site/andreaballatore
Contact: myname.mysurname@ucd.ie
Project home page: http://github.com/ucd-spatial

---------------------------------------------------------------------
	How to get Java
---------------------------------------------------------------------
To run the OpenStreetMap Wiki Crawler, Java 1.6+ has to be installed. 
The Java Runtime Enviroment can be downloaded at 
http://www.java.com/en/download

---------------------------------------------------------------------
	How to launch the precompiled jar
---------------------------------------------------------------------
* Linux / Unix / Mac OS X: open a shell, go to the file directory and type
	"./run_unix.sh".

* Microsoft Windows: open a command terminal, go to the file directory
	and run "./run_windows.sh".

---------------------------------------------------------------------
	How to get the Source
---------------------------------------------------------------------

The source code is available on GitHub: http://github.com/ucd-spatial

---------------------------------------------------------------------
	How to compile the source code
---------------------------------------------------------------------

The project was developed in the Eclipse IDE
* http://www.eclipse.org
With the Groovy plugin:
* http://groovy.codehaus.org/Eclipse+Plugin

The project can be imported into Eclipse through the 
"import existing projects" function.

The file build.xml contains Apache ANT commands to compile and run 
the crawler: http://ant.apache.org 

--------------------------------------------------------------------------------
     REFERENCE
--------------------------------------------------------------------------------

If you use this work in your research, please reference this paper:

================================================================================
 A. Ballatore, D.C. Wilson & M. Bertolotto, A Survey of Volunteered Open Ontologies in 
 the Semantic Geospatial Web, in Advanced Techniques in Web Intelligence - 3: Quality-based
 Information Retrieval, Studies in Computational Intelligence, Springer, IN PRESS, 2012.
================================================================================

  BIBTEX entry:
  @incollection{Ballatore:2012:survey,
     author={Ballatore, A. and Wilson, D.C. and Bertolotto, M.},
     title = {{A Survey of Volunteered Open Ontologies in the Semantic Geospatial Web}},
     booktitle = {Advanced Techniques in Web Intelligence - 3:
     				 Quality-based Information Retrieval},
     series = {Studies in Computational Intelligence},
     publisher = {Springer},
     note = {IN PRESS}, 
     year = {2012}
  }

---------------------------------------------------------------------
	License
---------------------------------------------------------------------

See LICENSE.txt.

---------------------------------------------------------------------
	External Libraries
---------------------------------------------------------------------
The OSM Wiki Crawler is dependent upon the following open source 
libraries (see 'lib' folder for the licenses):

arq-2.8.8.jar
bliki-core-3.0.18.jar
bliki-pdf-3.0.18.jar
commons-compress-1.0.jar
commons-lang-2.4.jar
commons-logging-1.1.1.jar
commons-validator-1.4-SNAPSHOT.jar
derby-10.9.1.jar
derbyclient-10.9.1.jar
groovy-all-1.7.8.jar
icu4j-3.4.4.jar
iri-0.8.jar
jakarta-oro-2.0.8.jar
jena-2.6.4.jar
junit-4.10.jar
log4j-1.2.14.jar
slf4j-api-1.5.8.jar
slf4j-log4j12-1.5.8.jar
tagsoup-1.2.jar
wstx-asl-3.2.9.jar
xercesImpl-2.7.1.jar
xom.jar

---------------------------------------------------------------------