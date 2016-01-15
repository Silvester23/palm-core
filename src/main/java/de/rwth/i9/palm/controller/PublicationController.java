package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.feature.publication.PublicationFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserWidget;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/publication" )
public class PublicationController
{
	private static final String LINK_NAME = "publication";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private PublicationFeature publicationFeature;

	@Autowired
	private SecurityService securityService;

	/**
	 * Get the publication page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView publicationPage( 
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
			@RequestParam( value = "id", required = false ) final String publicationId, 
			@RequestParam( value = "title", required = false ) final String title,
			final HttpServletResponse response ) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "publication", LINK_NAME );

		List<Widget> widgets = new ArrayList<Widget>();

		User user = securityService.getUser();

		if ( user != null )
		{
			List<UserWidget> userWidgets = persistenceStrategy.getUserWidgetDAO().getWidget( user, WidgetType.PUBLICATION, WidgetStatus.ACTIVE );
			for ( UserWidget userWidget : userWidgets )
			{
				Widget widget = userWidget.getWidget();
				widget.setColor( userWidget.getWidgetColor() );
				widget.setWidgetHeight( userWidget.getWidgetHeight() );
				widget.setWidgetWidth( userWidget.getWidgetWidth() );
				widget.setPosition( userWidget.getPosition() );

				widgets.add( widget );
			}
		} else
			widgets.addAll( persistenceStrategy.getWidgetDAO().getWidget( WidgetType.PUBLICATION, WidgetStatus.DEFAULT ));
		// assign the model
		model.addObject( "widgets", widgets );

		if ( publicationId != null )
			model.addObject( "targetId", publicationId );

		if ( title != null )
			model.addObject( "targetTitle", title );

		return model;
	}

	/**
	 * Get the list of publications based on the following parameters
	 * 
	 * @param query
	 * @param eventName
	 * @param eventId
	 * @param page
	 * @param maxresult
	 * @param response
	 * @return JSON Map
	 */
	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "publicationType", required = false ) String publicationType,
			@RequestParam( value = "authorId", required = false ) String authorId,
			@RequestParam( value = "eventId", required = false ) String eventId,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			@RequestParam( value = "source", required = false ) String source,
			@RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch,
			@RequestParam( value = "orderBy", required = false ) String orderBy,
			final HttpServletResponse response )
	{
		/* == Set Default Values== */
		if ( query == null ) 			query = "";
		if ( publicationType == null ) 	publicationType = "all";
		if ( page == null )				page = 0;
		if ( maxresult == null )		maxresult = 50;
		if ( fulltextSearch == null )	fulltextSearch = "yes";
		else							fulltextSearch = "no";
		if ( orderBy == null )
			orderBy = "citation";
		// Currently, system only provides query on internal database
		source = "internal";
			
		
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		if ( !publicationType.equals( "name" ) )
			responseMap.put( "publicationType", publicationType );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "fulltextSearch", fulltextSearch );
		responseMap.put( "orderBy", orderBy );
		
		Map<String, Object> publicationMap = publicationFeature.getPublicationSearch().getPublicationListByQuery( query, publicationType, authorId, eventId, page, maxresult, source, fulltextSearch, orderBy );
		
		if ( (Integer) publicationMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) publicationMap.get( "totalCount" ) );
			return publicationFeature.getPublicationSearch().printJsonOutput( responseMap, (List<Publication>) publicationMap.get( "publications" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}

	/**
	 * Get details( publication content ) from a publication
	 * 
	 * @param id
	 *            of publication
	 * @param uri
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/detail", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationDetail( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationDetail().getPublicationDetailById( id );
	}
	
	/**
	 * Get the basic statistic (publication type, language, etc) from a
	 * publication
	 * 
	 * @param id
	 *            of publication
	 * @param uri
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/basicstatistic", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationBasicStatistic( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationBasicStatistic().getPublicationBasicStatisticById( id );
	}
	
	/**
	 * Extract Pdf on a specific publication
	 * 
	 * @param id
	 *            of publication
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/pdfExtract", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> doPdfExtraction( 
			@RequestParam( value = "id", required = false ) final String id, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationManage().extractPublicationFromPdf( id );
	}
	
	@RequestMapping( value = "/pdfExtractTest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> doPdfExtractionTest( @RequestParam( value = "url", required = false ) final String url, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationApi().extractPfdFile( url );
	}

	@RequestMapping( value = "/htmlExtractTest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, String> doHtmlExtractionTest( @RequestParam( value = "url", required = false ) final String url, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return publicationFeature.getPublicationApi().extractHtmlFile( url );
	}

	/**
	 * Get list of PuiblicationTopic
	 * 
	 * @param id
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/topic", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationTopic( 
			@RequestParam( value = "id", required = false ) final String id, 
 @RequestParam( value = "maxRetrieve", required = false ) final String maxRetrieve, 
			final HttpServletResponse response)
	{
		return publicationFeature.getPublicationMining().getPublicationExtractedTopicsById( id, maxRetrieve );
	}
}