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

import java.io.IOException
import java.math.*
import java.util.zip.GZIPInputStream
import org.apache.log4j.*
import org.apache.commons.validator.routines.UrlValidator
import org.ucd.osmwikicrawler.crawler.Crawler
import org.ucd.osmwikicrawler.exceptions.RemoteServiceException
import org.ucd.osmwikicrawler.rdf.WikiRdf

/**
 * This service contains general purpose methods that can be used 
 * everywhere in the application.
 * 
 * @author Andrea Ballatore
 *
 */
class Utils {

    boolean transactional = false
	
	static def log = Logger.getLogger(Utils)
	static int SAVED_OBJECTS = 0
	static final int MIN_FREE_MEMORY_M = 6
	static final int CACHE_VALIDITY_DAYS = 1
	static final boolean USE_EMAIL = true
	static final boolean GEN_FILES = true
	final static String ERRORS_SEP = " \t\t\t  "
	final static String LOG_ROW = "\n============================================================\n"
	final static String LOG_PROGRESS = "..."
	
	final static public String CRAWLER_OUTPUT_FOLDER = "osm_wiki_crawler_output"

	final static String ENGLISH_STOP_WORDS = ["a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"];


	/**
	 * 
	 * @param text
	 * @param wrap
	 * @return
	 */
	String wrapText(String text,String wrap){
		assert wrap
		return wrap + text + wrap
	}
	
	
	/**
	 *
	 * @return folder where the wiki is downloaded.
	 */
	static String getPageCacheFolder(){
		return "./$CRAWLER_OUTPUT_FOLDER/temp/page_cache/"
	}
	
	/**
	*
	* @return
	*/
   static String getTempFolder(){
	   return "./$CRAWLER_OUTPUT_FOLDER/temp/"
   }
	
	
	/**
	 * 
	 * @return
	 */
	static String getOutputFolder(){
		return "./$CRAWLER_OUTPUT_FOLDER/"
	}
	
	/**
	 *
	 * @param uri
	 * @return
	 */
	static String getDumpFileName( String uri, String folder = null ){
		assert uri
		if (!folder) folder = getPageCacheFolder()
		String fn = folder + URLEncoder.encode(uri)
		fn = fn.replaceAll('\\*',Crawler.ASTERISK_URL_ENC)
		return fn
	}
	
	/**
	 * 
	 * @param filePath
	 * @return
	 */
	public static String getUriFromFileName( String filePath ){
		try {
			String uri = URLDecoder.decode(filePath)
			//log.info("getUriFromFileName: "+uri)
			//Utils.validateUrl(uri)
			return uri
		} catch( java.lang.IllegalArgumentException e ){
			//log.warn("Illegal URI, skipping: $filePath")
			return null
		}
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	String removeAnchorFromUri( String uri ){
		assert uri
		int hash = uri.count("#")
		if ( hash == 0 ) return uri
		int firstHash = uri.indexOf("#")
		assert firstHash > 0
		String cleanUri = uri[0 .. firstHash-1]
		return cleanUri
	}

	/**
	 * 
	 * @param text
	 * @param targetFilePath
	 * @return
	 */
	public static void outputFile( String text, String targetFilePath, boolean silentMode = false ){
		assert text != null
		assert targetFilePath
		try{
			FileOutputStream file = new FileOutputStream( targetFilePath )
			assert file
			file.write( text.getBytes() )
			file.close()
		} catch (IOException e) {
			if ( silentMode ){
				log.error "failed to write in file ${targetFilePath}",e
			} else {
				throw e
			}
		}
	}
	
	/**
	 * 
	 * @param fileName
	 * @return true if page is not older than one day
	 */
	static boolean isCachedPageStillValid( String fileName ){
		assert fileName
		File file = new File( fileName )
		if (!file.exists()) return false
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, CACHE_VALIDITY_DAYS * -1);
		long purgeTime = cal.getTimeInMillis();
		long fileTime = file.lastModified() 
		if ( fileTime < purgeTime ){
			log.debug("old cache entry detected $fileName")
			return false
		} else {
			return true
		}
	}
	
	static boolean validUriLength( String uri ){
		assert uri != null
		return uri.length() <= Crawler.OSM_URI_MAX_LENGTH && uri.length() >= Crawler.OSM_URI_MIN_LENGTH
	}
	
	/**
	 * 
	 * @param gzipped
	 * @return
	 * @throws IOException
	 */
	public static String decompressGzipText( String gzipped ) throws IOException {
		//public static String decompress(byte[] compressed) throws IOException {
		final int BUFFER_SIZE = 32;
		// convert string to bytes
		byte[] bytes = gzipped.getBytes()
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
		StringBuilder string = new StringBuilder();
		byte[] data = new byte[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = gis.read(data)) != -1) {
			string.append(new String(data, 0, bytesRead));
		}
		gis.close();
		is.close();
		return string.toString();
	
	}
	
	/**
	 * 
	 * @param gzFilePath
	 * @param outFile
	 * @return
	 * @throws IOException
	 */
	public static boolean gunzipFile( String gzFilePath, String outFile ) throws IOException {
		
		String source = gzFilePath;
		String outFilename = outFile;
			
		FileInputStream instream= new FileInputStream(source);
		GZIPInputStream ginstream = new GZIPInputStream(instream);
		FileOutputStream outstream = new FileOutputStream(outFilename);
		byte[] buf = new byte[1024];
		int len;
		while ((len = ginstream.read(buf)) > 0){
			outstream.write(buf, 0, len);
		}

		ginstream.close();
		outstream.close();
		return true
	}
	
	/**
	 * 
	 * @param urisStr a string containing space separated URIs 
	 * @return a string containing space separated valid URIs
	 */
	static String fixUrisString( String urisStr ){
		if (!urisStr) return urisStr
		assert urisStr
		def fixedStrArr = []
		urisStr = urisStr.trim()
		def uris = urisStr.split(' ')*.trim()
		if (uris.size()>1)
			int i = 0 
		uris.each{
			assert it,"null element found in urisStr=$urisStr"
			if (!validUriLength(it)){
				log.warn("fixUrisString: detected very long or very short URI (max length=$Crawler.OSM_URI_MAX_LENGTH): skipping... $it")
			}  
			else {
				fixedStrArr.add( Utils.fixUri(it.trim()) )
			}
		}
		return fixedStrArr.join(' ')
	}
	
	
	/**
	 * 
	 * @param uri any URI
	 * @return a valid URI
	 */
	static String fixUri( String uri, boolean validate = true ){
		assert uri
		// fix issue in some Wikipedia links
		if (Crawler.isWikipediaLink(uri)){
			uri = uri.replaceAll("http://en.wikipedia.org/wiki/http://en.wikipedia.org/wiki/","http://en.wikipedia.org/wiki/" )
			return uri
		}
		
		assert Crawler.isOsmWikiUrl(uri)
		if (!validUriLength(uri)){
			log.warn("fixUri: detected very long/short URI")
			uri = Crawler.fixLongOsmWikiUri(uri)
			//throw new IllegalArgumentException("very long or short URI (max length=$Crawler.OSM_URI_MAX_LENGTH): $uri")
		}
		String fixedUri = uri
		
		fixedUri = fixedUri.replaceAll( "\\>", "" )
		fixedUri = fixedUri.replaceAll( "\\<", "" )
		fixedUri = fixedUri.replaceAll( "\\[", "" )
		fixedUri = fixedUri.replaceAll( "\\]", "" )
		fixedUri = fixedUri.replaceAll( ";", "" )
		fixedUri = fixedUri.replaceAll( "\n", "," )
		fixedUri = fixedUri.replaceAll( ",_", "," )
		fixedUri = fixedUri.replaceAll( "_,", "," )
		fixedUri = fixedUri.replaceAll( "=%3D", "%3D" )
		fixedUri = fixedUri.replaceAll( "_=_", "%3D" )
		
		fixedUri = fixedUri.replaceAll( "\\.\\.\\.", "" )
		fixedUri = fixedUri.replaceAll( "\"", "" )
		fixedUri = fixedUri.replaceAll( "\\|", "_" )
		fixedUri = fixedUri.replaceAll( "\\+", "%2B" )
		fixedUri = fixedUri.replaceAll( "\\?", "%3F" )
		
		if (fixedUri != uri){
			log.debug("fixUri: bad uri detected ($uri): replaced with $fixedUri")
		}
		if (validate) validateUrl(fixedUri)
		return fixedUri
	}
	
	/**
	 * 
	 * @param e
	 * @return a string containing a stack trace log
	 */
	static String stack2string( Exception e ) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "------\r\n" + sw.toString() + "------\r\n";
		}
		catch(Exception e2) {
			return "bad stack2string";
		}
	}
	
	/**
	 * @param filePath path to text file
	 * @return file content
	 */
	static String readFile( String filePath ){
		assert filePath
		try{
			File file = new File( filePath )
			String content = file.getText()
			return content
		} catch (IOException e) {
			throw new Exception( "failed to read from file '${filePath}': " + e )
		}
	}
	
	/**
	 * 
	 * @return a random offset, either positive or negative. range: [-999,-1] or [1,999]
	 */
	double getRandomOffset(){
		def upper = 999
		def lower = 1
		def randomInt = new Random().nextInt(upper - lower) + lower
		def randomInt2 = new Random().nextInt(upper - lower) + lower
		def offset = (randomInt2 % 2 == 0) ? randomInt : -randomInt
		assert offset != 0
		return offset
	}
	/**
	 * 
	 * @param minValue
	 * @param maxValue
	 * @return random integer within range
	 */
	Integer getRandomInteger( Integer minValue, Integer maxValue ){
		Random rand = new Random()
		int randInt = rand.nextInt( maxValue )
		if ( randInt < minValue ){
			randInt += minValue
		}
		assert randInt >= minValue && randInt <= maxValue
		return randInt
	}
	
	/**
	 * 
	 * @return a random Date (past or future)
	 */
	Date getRandomDate(){
		long lower = 0
		long upper = 12681390
		
		long randomLong = new Date().getTime() + getRandomOffset() * 100000000

		def date = new Date( randomLong )
		assert date
		return date
	}
	
	/**
	 * 
	 * @return lines from log containing errors
	 */
	String getErrorsFromLogFile( fileName, String search ){
		assert fileName
		File file = new File( fileName )
		assert file.exists()
		String content = ''
		file.eachLine{ line ->
			boolean errors = false
			if (search && !search.isEmpty()){
				// sarch string
				if (line.toLowerCase().trim().contains(search.trim().toLowerCase()))
					errors = true
			} else {
					// search for errors
					if (line.toLowerCase().trim().contains("error")){
						errors = true
					}
					if (line.toLowerCase().trim().contains("exception")){
						errors = true
					}
					if (line.toLowerCase().trim().contains("fail")){
						errors = true
					}
			}
			if (errors)
				content = line + "\n" + content 
		}
		if (content.isEmpty()) content = "nothing found"
		return content
	}
	
	/**
	 * 
	 * @param f
	 * @return
	 */
	static String formatFloat( Double f ){
		return String.format('%9f', f).trim()
	}
	
	/**
	 * 
	 * @param f
	 * @param precision
	 * @return
	 */
	static String roundFormatFloat( Double f, int precision ){
		assert f != null
		assert precision >= 0
		
		if ( f > 0 && f < 0.0000000000000001 )
			return "0.0"
			
		MathContext mc = new MathContext(precision)
		BigDecimal d = f
		String res = d.round( mc ).toString()
		return res
	}
	
	/**
	 * 
	 * @param f
	 * @return
	 */
	Float preventUnderflow( Double f ){
		if ( f == null ) return null
		assert f != null
		if (f < 0.00000000000000000001)
			return 0.0
		else return f
	}
	
	/**
	 * Cut string at character maxChar
	 * 
	 * @param text
	 * @param maxChar
	 * @return cut string
	 */
	String cutString( String text, int maxChar ){
		if (!text) return ''
		assert maxChar >= 1
		if (text.length() > maxChar ){
			return text[0..maxChar] + "..."
		} else {
			return text
		}
	}
	
	/**
	 * 
	 * @return
	 */
	String logMemory(){
		final int MIN_MEMORY_MB = 5
		String msg = ""
		def totMem = Runtime.getRuntime().totalMemory() / 1000000
		def freeMem = Runtime.getRuntime().freeMemory() / 1000000
		msg = LOG_ROW+"=== log MEMORY: totMem=${totMem}; freeMem=${freeMem} ==="
		log.info(msg)
		if (freeMem < MIN_MEMORY_MB){
			log.warn("=== LOW MEMORY: ${freeMem} ===")
		}
		return msg
	}
	
	/**
	 * Very important function to compare doubles and doubles.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	boolean equalsDec( def a, def b ){
		final double TOLERANCE = 0.00000001
		def tmp = Math.abs( a-b )
		return tmp < TOLERANCE
	}
	
	/**
	 * 
	 */
	void forceGC(){
		//log.debug("=== forceGC: forcing garbage collector...")
		def freeMem = Runtime.getRuntime().freeMemory()
		Runtime.getRuntime().gc()
		def freeMemAfter = Runtime.getRuntime().freeMemory()
		def freed = ( freeMem - freeMemAfter ) / 1000000
		if (freed < 0)
			freed = -freed
		log.debug(LOG_ROW+"* forceGC: garbage collector: FREED ${freed}M"+LOG_ROW)
	}
	
	/**
	 * 
	 */
	void checkForMemoryLeak(){
		def freeMem = Runtime.getRuntime().freeMemory()
		def freed = ( freeMem ) / 1000000
		assert freed >= 0
		if (freed < MIN_FREE_MEMORY_M){
			// heap is full, stop thread
			throw new RuntimeException("***** Too little memory (${freed}M) to keep on working *****")
		}
	}
	
	/**
	 * 
	 * @param url
	 * @param maxRetrials
	 * @return
	 */
	static boolean downloadURLBinary( String url, String targetFile, int maxRetrials = 10){
		assert url
		assert targetFile
		assert maxRetrials >= 0
		def file = new FileOutputStream( targetFile )
		def out = new BufferedOutputStream( file )
		out << new URL(url).openStream()
		out.close()
		return true
	}

	/**
	 * Download URL and return file content.
	 * 
	 * @param url
	 * @return
	 */
	static String downloadURL( String url, int maxRetrials ){
		assert url
		log.info("downloadURL: $url")
		assert maxRetrials > 0 && maxRetrials < 30
		String msg = "downloadURL: url=${url},maxRetrials=${maxRetrials}"
		int retrial = maxRetrials
		while (retrial > 0){
			retrial--
			URL urlObj = new URL( url )
			HttpURLConnection connection = (HttpURLConnection)urlObj.openConnection()
			String result = null
			if(connection.responseCode == 200){
				result = connection?.content?.text
				if (!result){
					throw new Exception(msg+ ",i=${retrial} connection?.content?.text is null")
				}				
			} else{
				//throw new Exception(msg+" failed: code ${connection.responseCode}")
			}
			connection.disconnect()
			return result
		}
		throw new Exception(msg+" failed after all trials.")
	}
	
	/**
	 * SPARQL cache. Used to prevent hammering remote endpoints
	 * 
	 * see getSparqlCacheEntry
	 * @param uri
	 * @return
	 */
	static String getSparqlCacheEntryByUri( String uri ){
		assert uri
		WebPage wp = WebPage.findByUri( uri )
		if (!wp){
			return null
		}
		log.debug("getSparqlCacheEntryByUri: entry found.")
		return wp?.content
	}
	
	/**
	 * SPARQL cache. Used to prevent hammering remote endpoints
	 * 
	 * see getSparqlCacheEntryByUri
	 * @param uri
	 * @param result
	 */
	void storeSparqlCacheEntry( String uri, String result ){
		log.debug("storeSparqlCacheEntry")
		assert uri
		assert result != null
		WebPage wp = WebPage.findByUri( uri )
		// check if result is different. if it is, store it again
		boolean toBeUpdated = true
		if (wp){
			if (wp.content == result.trim()){
				// don't update page
				toBeUpdated = false
			}
		} else {
			wp = new WebPage()
			wp.uri = uri
		}
		
		if (toBeUpdated){
			wp.content = result.trim()
			wp.downloadFailed = false
			saveObj( wp, true )
		}
	}
	
	/**
	 * 
	 * @param uri
	 * @return page content
	 */
	static public WebPage getWebPageByURI( String uri, boolean failOnEmptyResponse = false ){
		assert uri
		
		WebPage wp = null //WebPage.findByUri( uri )
		final int RETRIALS_N = 5
		final int DOWNLOAD_RETRIAL_SLEEP_MS = 500

		assert !wp
		// get new page and save it
		wp = new WebPage()
		wp.uri = uri
		try{
			wp.content = downloadURL( uri, RETRIALS_N )
			wp.downloadFailed = false
		} catch(e){
			log.warn("getWebPageByURI: "+uri+": \t"+e)
			wp.downloadFailed = true
		}
		
		// make sure that the downloaded page is not empty
		if (failOnEmptyResponse){
			int retrialLeft = RETRIALS_N
			int curDelay = DOWNLOAD_RETRIAL_SLEEP_MS
			while(retrialLeft > 0 && !wp.content){
				log.warn( "getWebPageByURI: failed on empty response. Retrying (i=${retrialLeft}, delayMs=${curDelay})..." )
				try{
					wp.content = downloadURL( uri, RETRIALS_N )
				} catch(e){
					log.warn("getWebPageByURI: retrial failed: "+e)
				}
				retrialLeft--
				curDelay += DOWNLOAD_RETRIAL_SLEEP_MS
				assert curDelay > 0 && curDelay < 900000
				Thread.currentThread().sleep( curDelay )
			}
			if (!wp.content){
				throw new RemoteServiceException( "getWebPageByURI: empty response for uri="+uri )
			} else {
				wp.downloadFailed = false
			}
			assert !wp.downloadFailed
		}
		log.debug( "getWebPageByURI: downloaded page uri=${uri}" )
		return wp
	}
	
	/**
	 * 
	 * @param uri
	 */
	private void markUriAsDownloaded( WebPage wp ){
		assert wp
		assert wp.uri
		wp.rdfImported = true
		log.debug("markUriAsDownloaded: '${wp.uri}'")
		saveObj( wp, true )
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	private boolean isUriDownloaded( String uri ){
		assert uri
		WebPage wp = WebPage.findByUri( uri )
		if (!wp) return false 
		else return wp.rdfImported
	}
	
	/**
	 * 
	 * @return path to resources
	 */
	String getResourcePath(){
		assert ConfigurationHolder.config.semanticopenstreetmap.resource.folder
		/*
		 assert grailsApplication.metadata['app.name']
		 String baseDir = getApplicationPath()
		 if (baseDir[-1]!="\\" && baseDir[-1]!="/" ){
		 baseDir += "\\"
		 }
		 if (baseDir.indexOf(grailsApplication.metadata['app.name'])<0){
		 baseDir += grailsApplication.metadata['app.name']
		 }
		 */
		def baseDir = ConfigurationHolder.config.semanticopenstreetmap.resource.folder
		if (baseDir[-1]!="\\" && baseDir[-1]!="/" ){
			baseDir += "\\"
		}
		return baseDir
	}
	
	/**
	 * 
	 * @return
	 */
	static String getSemantiNetworkOutputFolder(){
		return "./$CRAWLER_OUTPUT_FOLDER/semantic_network/"
	}
	
	/**
	 * 
	 * @param template
	 * @param sub
	 * @param body
	 */
	void sendEmail( String email, String sub, String body ){
		assert email
		assert sub
		
		if (!USE_EMAIL) return

		mailService.sendMail {
			to email
			subject sub
			html body
		}
	}

	/**
	 * Validate a url through the Apache commons UrlValidator.
	 *  
	 * @param url a url to validate
	 */
	static boolean validateUrl( String url, boolean throwException = true ){
		if (!url)
			throw new IllegalArgumentException("validateUrl: empty url")
		UrlValidator urlValidator = new UrlValidator()
		assert urlValidator
		boolean valid = urlValidator.isValid(url) 
		if ( throwException && !valid )
			throw new IllegalArgumentException("validateUrl: url '${url}' is not valid")
		return valid
	}
	
	/**
	 * 
	 * @param paramRange [min, max, increment]
	 * 
	 */
	void validateParameterBatchRange( def paramRange ){
		assert paramRange.size() == 3
		assert paramRange[0] < paramRange[1]
		assert paramRange[3] <= paramRange[2]
	}
	
	/**
	 * Split camel case string.
	 * 
	 * @param camelCaseText
	 * @return
	 */
	static String splitCamelCaseString( String camelCaseText ){
		assert camelCaseText,"camelCaseText is null"
		// fix for first character
		String splitText = ( camelCaseText =~ /([A-Z][a-z]+)/ ).replaceAll('$1 ')
		splitText = ( camelCaseText =~ /([A-Z]?[a-z]+)/ ).replaceAll('$1 ')
		return splitText
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	static String clearTextFromSpecialChars( String text ){
		if (!text) return ""
		//def REGEX_ALPHANUM = /([^A-Za-z0-9])/
		def REGEX_SPACES = /(\s+)/
		//text = text.replaceAll( REGEX_ALPHANUM, ' ').trim();
		text = text.toLowerCase().replaceAll(/[^\p{javaLowerCase}]/, ' ').replace("_"," ")
		text = text.replaceAll( REGEX_SPACES, ' ').trim();
		return text
	}
	
	/**
	 * Resolve an http redirection.
	 *  
	 * @param uri
	 * @return real uri or null
	 */
	static String getLinkRedirection( String uri ){
		assert uri
		
		URL urlObj = new URL( uri )
		HttpURLConnection con = (HttpURLConnection)urlObj.openConnection()
		con.setInstanceFollowRedirects( false )
		con.connect()
		String redirectedUri = null
		int responseCode = con.getResponseCode()
		if (responseCode > 299 && responseCode < 400 ){
			String locUri = con.getHeaderField( "Location" )
			return locUri
		}
		return null
	}
	
	/**
	 * 
	 * @return
	 */
	String getImageCachePath(){
		getResourcePath() + "images/image_cache/"
	}
	
	/**
	 * 
	 * @return true if OS is windows 
	 */
	boolean isOsWindows(){
		return org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS
	}
	
	/**
	 * 
	 * @return true if OS is unix 
	 */ 
	boolean isOsUnix(){
		return org.apache.commons.lang.SystemUtils.IS_OS_UNIX
	}
	
	/**
	 * TODO: find stop words in authoritative source
	 * @param word
	 * @return true if word is an English stop word.
	 */
	boolean isStopWord( String word ){
		assert word
		String w = word.toLowerCase().trim()
		assert w
		boolean sw = ENGLISH_STOP_WORDS.contains( w )
		//log.debug("isStopWord( $word )==$sw")
		return sw
	}
}

