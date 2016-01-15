package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.datasetcollect.service.PublicationCollectionService;
import de.rwth.i9.palm.feature.researcher.ResearcherFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.AuthorSource;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@RequestMapping( value = "/researcher" )
public class ResearcherController
{

	private static final String LINK_NAME = "researcher";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;
	
	@Autowired
	private ResearcherFeature researcherFeature;
	
	@Autowired
	private PublicationCollectionService publicationCollectionService;

	/**
	 * Landing page of researcher page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView researcherPage( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name,
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = TemplateHelper.createViewWithLink( "researcher", LINK_NAME );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.RESEARCHER, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );
		// assign query
		if ( id != null )
			model.addObject( "targetId", id );
		if ( name != null )
			model.addObject( "targetName", name );
		return model;
	}

	/**
	 * Get list of author given query ( author name )
	 * 
	 * @param query
	 * @param page
	 * @param maxresult
	 * @param response
	 * @return JSON Maps of response with researcher list
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws OAuthProblemException 
	 * @throws OAuthSystemException 
	 * @throws org.apache.http.ParseException 
	 */
	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getAuthorList(
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "queryType", required = false ) String queryType,
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			@RequestParam( value = "source", required = false ) String source,
			@RequestParam( value = "addedAuthor", required = false ) String addedAuthor,
			@RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch,
			@RequestParam( value = "persist", required = false ) String persist,
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		/* == Set Default Values== */
		if ( query == null ) 			query = "";
		if ( queryType == null ) 		queryType = "name";
		if ( startPage == null )		startPage = 0;
		if ( maxresult == null )		maxresult = 50;
		if ( source == null )			source = "internal";
		if ( addedAuthor == null )
			addedAuthor = "no";
		if ( fulltextSearch == null )	fulltextSearch = "no";
		if ( persist == null )			persist = "no";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		if ( !queryType.equals( "name" ) )
			responseMap.put( "queryType", queryType );
		responseMap.put( "page", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );
		if ( !fulltextSearch.equals( "no" ) )
			responseMap.put( "fulltextSearch", fulltextSearch );
		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}
		if ( addedAuthor.equals( "yes" ) )
			responseMap.put( "addedAuthor", addedAuthor );
		
		Map<String, Object> authorsMap = researcherFeature.getResearcherSearch().getResearcherMapByQuery( query, queryType, startPage, maxresult, source, addedAuthor, fulltextSearch, persistResult );
		
		// store in session
		if ( source.equals( "external" ) )
			request.getSession().setAttribute( "authors", authorsMap.get( "authors" ) );
		
		if ( (Integer) authorsMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) authorsMap.get( "totalCount" ) );
			return researcherFeature.getResearcherSearch().printJsonOutput( responseMap, (List<Author>) authorsMap.get( "authors" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}
	
	@Transactional
	@RequestMapping( value = "/session", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getAuthorFromSession( 
			HttpServletRequest request,
			HttpServletResponse response ) throws IOException, InterruptedException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
		@SuppressWarnings( "unchecked" )
		List<Author> authors = (List<Author>) request.getSession().getAttribute( "authors" );
		
		if ( authors != null && !authors.isEmpty() )
		{
			List<AuthorSource> authorSources = new ArrayList<AuthorSource>( authors.get( 0 ).getAuthorSources() );

			return researcherFeature.getResearcherSearch().printJsonOutput( responseMap, authors );
		}
		else
			return Collections.emptyMap();
	}
	
	/**
	 * Fetch author data, mining author information and publication from
	 * academic network if necessary
	 * 
	 * @param id
	 * @param name
	 * @param uri
	 * @param affiliation
	 * @param force
	 * @param response
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws ParseException
	 * @throws TimeoutException 
	 * @throws org.apache.http.ParseException 
	 * @throws OAuthProblemException 
	 * @throws OAuthSystemException 
	 */
	@RequestMapping( value = "/fetch", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherFetchNetworkDataset( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "affiliation", required = false ) final String affiliation,
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force,
			HttpServletRequest request,
			HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, ParseException, TimeoutException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		@SuppressWarnings( "unchecked" )
		List<Author> sessionAuthors = (List<Author>) request.getSession().getAttribute( "authors" );
		
		return researcherFeature.getResearcherMining().fetchResearcherData( id, name, uri, affiliation, pid, force, sessionAuthors );
	}
	
	/**
	 * Get author interest
	 * 
	 * @param authorId
	 * @param name
	 * @param extractionServiceType
	 * @param startDate
	 * @param endDate
	 * @param response
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws URISyntaxException
	 * @throws ParseException
	 */
	@RequestMapping( value = "/interest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String authorId, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "extractType", required = false ) final String extractionServiceType,
			@RequestParam( value = "startDate", required = false ) final String startDate,
			@RequestParam( value = "endDate", required = false ) final String endDate,
			final HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException
	{
		if ( name != null )
			return researcherFeature.getResearcherInterest().getAuthorInterestByName( name, extractionServiceType, startDate, endDate );
		else
			return researcherFeature.getResearcherInterest().getAuthorInterestById( authorId, extractionServiceType, startDate, endDate );
		// return Collections.emptyMap();
	}
	
	@RequestMapping( value = "/enrich", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherEnrich( @RequestParam( value = "id", required = false ) final String authorId, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException, TimeoutException
	{
		// id=90522536-4717-4fa3-ac43-b3f6300ad6c4
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );
		publicationCollectionService.enrichPublicationByExtractOriginalSources( new ArrayList<Publication>( author.getPublications() ), author, true );
		return Collections.emptyMap();
	}

	@RequestMapping( value = "/interestEvolution", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterestEvolution( 
			@RequestParam( value = "id", required = false ) final String authorId, 
			@RequestParam( value = "name", required = false ) final String name, 
			@RequestParam( value = "extractType", required = false ) final String extractionServiceType,
			@RequestParam( value = "startDate", required = false ) final String startDate,
			@RequestParam( value = "endDate", required = false ) final String endDate,
			final HttpServletResponse response ) 
					throws InterruptedException, IOException, ExecutionException
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		
//		// get author
//		Author author = this.getTargetAuthor( responseMap, id, name, uri, affiliation );
//		if( author == null )
//			return responseMap;

		return null;
	}
	
	@RequestMapping( value = "/autocomplete", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationBasicStatistic( 
			@RequestParam( value = "name", required = false ) final String name, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException, org.apache.http.ParseException, OAuthSystemException, OAuthProblemException
	{
		return researcherFeature.getResearcherApi().getAuthorAutoCompleteFromNetworkAndDb( name );
	}
	
	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "id", required = false ) final String authorId,
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			final HttpServletResponse response)
	{
		return researcherFeature.getResearcherPublication().getPublicationListByAuthorId( authorId );
	}

	@RequestMapping( value = "/coAuhtorList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCoAuthorList( 
			@RequestParam( value = "id", required = false ) final String authorId, 
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, 
			final HttpServletResponse response)
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( authorId == null || authorId.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "authorId null" );
			return responseMap;
		}

		// get author
		Author author = persistenceStrategy.getAuthorDAO().getById( authorId );

		if ( author == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.putAll( researcherFeature.getResearcherCoauthor().getResearcherCoAuthorMap( author ) );

		return responseMap;
	}

}