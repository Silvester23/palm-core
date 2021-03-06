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

import de.rwth.i9.palm.feature.circle.CircleFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.CircleWidget;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserCircleBookmark;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/circle" )
public class CircleController
{
	private static final String LINK_NAME = "circle";

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private CircleFeature circleFeature;

	/**
	 * Get the circle page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView circlePage( 
			@RequestParam( value = "id", required = false ) final String circleId, 
			@RequestParam( value = "name", required = false ) String name,
			final HttpServletResponse response ) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "circle", LINK_NAME );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CIRCLE, "sidebar" );
		// assign the model
		model.addObject( "widgets", widgets );

		if ( circleId != null )
		{
			model.addObject( "targetId", circleId );
			if ( name == null )
			{
				Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );
				if ( circle != null )
					name = circle.getName();
			}
		}

		if ( name != null )
			model.addObject( "targetName", name );

		return model;
	}
	
	/**
	 * Get the circle page
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@RequestMapping( value = "/widgetContent", method = RequestMethod.GET )
	@Transactional
	public ModelAndView circleContentWidget( 
			@RequestParam( value = "id", required = false ) final String circleId, 
			@RequestParam( value = "name", required = false ) final String name,
			final HttpServletResponse response ) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "widgetLayoutMainContent", LINK_NAME );

		List<Widget> widgets = new ArrayList<Widget>();

		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		if ( circle == null )
		{
			model = TemplateHelper.createViewWithLink( "404", "error" );
			model.addObject( "erorMessage", "Circle with id " + circleId + " not found" );
			return model;
		}

		List<CircleWidget> circleWidgets = persistenceStrategy.getCircleWidgetDAO().getWidget( circle, WidgetType.CIRCLE, WidgetStatus.ACTIVE );
		for ( CircleWidget circleWidget : circleWidgets )
		{
			Widget widget = circleWidget.getWidget();
			widget.setColor( circleWidget.getWidgetColor() );
			widget.setWidgetHeight( circleWidget.getWidgetHeight() );
			widget.setWidgetWidth( circleWidget.getWidgetWidth() );
			widget.setPosition( circleWidget.getPosition() );

			widgets.add( widget );
		}

		// assign the model
		model.addObject( "widgets", widgets );

		if ( circleId != null )
			model.addObject( "targetId", circleId );

		if ( name != null )
			model.addObject( "targetName", name );

		return model;
	}

	/**
	 * Get the list of circles based on the following parameters
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
	public @ResponseBody Map<String, Object> getCircleList( 
			@RequestParam( value = "id", required = false ) String circleId,
			@RequestParam( value = "creatorId", required = false ) String creatorId,
			@RequestParam( value = "query", required = false ) String query,
			@RequestParam( value = "page", required = false ) Integer page, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult, @RequestParam( value = "fulltextSearch", required = false ) String fulltextSearch, @RequestParam( value = "orderBy", required = false ) String orderBy,
			final HttpServletResponse response )
	{
		/* == Set Default Values== */
		if ( query == null )
			query = "";
		if ( page == null )
			page = 0;
		if ( maxresult == null )
			maxresult = 50;
		if ( fulltextSearch == null )
			fulltextSearch = "no";
		else
			fulltextSearch = "yes";
		if ( orderBy == null )
			orderBy = "citation";

		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		responseMap.put( "query", query );
		responseMap.put( "page", page );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "orderBy", orderBy );

		Map<String, Object> circleMap = circleFeature.getCircleSearch().getCircleListByQuery( query, creatorId, page, maxresult, fulltextSearch, orderBy );

		return circleFeature.getCircleSearch().printJsonOutput( responseMap, (List<Circle>) circleMap.get( "circles" ) );

	}

	/**
	 * Get details( circle content ) from a circle
	 * 
	 * @param id
	 *            of circle
	 * @param uri
	 * @param response
	 * @return JSON Map
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ExecutionException
	 */
	@RequestMapping( value = "/detail", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCircleDetail( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "uri", required = false ) final String uri,
			@RequestParam( value = "retrieveAuthor", required = false ) String retrieveAuthor,
			@RequestParam( value = "retrievePubication", required = false ) String retrievePublication, 
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		boolean isRetrieveAuthorDetail = false;
		if ( retrieveAuthor != null && retrieveAuthor.equals( "yes" ) )
			isRetrieveAuthorDetail = true;

		boolean isRetrievePublicationDetail = false;
		if ( retrievePublication != null && retrievePublication.equals( "yes" ) )
			isRetrievePublicationDetail = true;

		return circleFeature.getCircleDetail().getCircleDetailById( id, isRetrieveAuthorDetail, isRetrievePublicationDetail );
	}

	/**
	 * Get basic information of circle
	 * 
	 * @param circleid
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/basicInformation", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getBasicInformationMap( 
			@RequestParam( value = "id", required = false ) final String circleid, 
			final HttpServletResponse response)
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( circleid == null || circleid.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circleid null" );
			return responseMap;
		}

		// get author
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleid );
		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.put( "circle", circleFeature.getCircleBasicInformation().getCircleBasicInformationMap( circle ) );

		// check whether circle is already booked or not
		User user = securityService.getUser();
		if ( user != null )
		{
			UserCircleBookmark ucb = persistenceStrategy.getUserCircleBookmarkDAO().getByUserAndCircle( user, circle );
			if ( ucb != null )
				responseMap.put( "booked", true );
			else
				responseMap.put( "booked", false );
		}

		return responseMap;
	}

	@RequestMapping( value = "/researcherList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCircleResearcher( 
			@RequestParam( value = "id", required = false ) final String circleid, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response)
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( circleid == null || circleid.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circleid null" );
			return responseMap;
		}

		// get author
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleid );
		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.putAll( circleFeature.getCircleResearcher().getCircleResearcherMap( circle ) );

		return responseMap;
	}
	
	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getCirclePublication( 
			@RequestParam( value = "id", required = false ) final String circleid,
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response)
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( circleid == null || circleid.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circleid null" );
			return responseMap;
		}

		// get author
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleid );
		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circle not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.putAll( circleFeature.getCirclePublication().getCirclePublicationMap( circle ) );

		return responseMap;
	}
	
	/**
	 * Get PublicationMap (JSON), containing publications basic information and detail.
	 * @param authorId
	 * @param startPage
	 * @param maxresult
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/publication", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "id", required = false ) final String circleId,
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "year", required = false ) String year,
			@RequestParam( value = "orderBy", required = false ) String orderBy,
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			final HttpServletResponse response)
	{
		if ( year == null )				year = "all";
		if ( query == null )			query = "";
		if ( orderBy == null )			orderBy = "date";
		
		return circleFeature.getCirclePublication().getCirclePublicationByCircleId( circleId, query, year, startPage, maxresult, orderBy );
	}
	
	@RequestMapping( value = "/interest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String circleId, 
			@RequestParam( value = "updateResult", required = false ) final String updateResult,
			final HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException
	{
		boolean isReplaceExistingResult = false;
		if ( updateResult != null && updateResult.equals( "yes" ) )
			isReplaceExistingResult = true;
		return circleFeature.getCircleInterest().getCircleInterestById( circleId, isReplaceExistingResult );
	}
	
	@RequestMapping( value = "/topicModel", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> circleTopicModel( @RequestParam( value = "id", required = false ) final String circleId, @RequestParam( value = "updateResult", required = false ) final String updateResult, final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException
	{
		if ( circleId != null )
		{
			boolean isReplaceExistingResult = false;
			if ( updateResult != null && updateResult.equals( "yes" ) )
				isReplaceExistingResult = true;

			return circleFeature.getCircleTopicModeling().getLdaBasicExample( circleId, isReplaceExistingResult );
		}
		return Collections.emptyMap();
	}

	/**
	 * Get PublicationMap (JSON), containing top publications (highly cited publications) information and detail.
	 * @param circleId
	 * @param startPage
	 * @param maxresult
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/publicationTopList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationTopList( 
			@RequestParam( value = "id", required = false ) final String authorId,
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			final HttpServletResponse response)
	{	
		if( startPage == null ) startPage = 0;
		if( maxresult == null ) maxresult = 10;
		return circleFeature.getCircleTopPublication().getTopPublicationListByCircleId( authorId, startPage, maxresult );
	}
	
	/**
	 * Get academic event tree of given circle
	 * 
	 * @param circleId
	 * @param startPage
	 * @param maxresult
	 * @param response
	 * @return
	 */
	@RequestMapping( value = "/academicEventTree", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getAcademicEventTreeMap( 
			@RequestParam( value = "id", required = false ) final String circleId,
			final HttpServletResponse response)
	{
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		if ( circleId == null || circleId.equals( "" ) )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "circleId null" );
			return responseMap;
		}

		// get author
		Circle circle  = persistenceStrategy.getCircleDAO().getById( circleId );

		if ( circle == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "statusMessage", "author not found in database" );
			return responseMap;
		}

		// get coauthor calculation
		responseMap.putAll( circleFeature.getCircleAcademicEventTree().getCircleAcademicEventTree( circle ) );

		return responseMap;
	}

}