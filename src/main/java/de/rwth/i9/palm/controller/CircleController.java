package de.rwth.i9.palm.controller;

import java.io.IOException;
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
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;

@Controller
@SessionAttributes( { "sessionDataSet" } )
@RequestMapping( value = "/circle" )
public class CircleController
{
	private static final String LINK_NAME = "circle";

	@Autowired
	private ApplicationContextService appService;

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
			@RequestParam( value = "sessionid", required = false ) final String sessionId, 
 @RequestParam( value = "id", required = false ) final String circleId, @RequestParam( value = "name", required = false ) final String name,
			final HttpServletResponse response ) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "circle", LINK_NAME, sessionDataSet );

		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CIRCLE, WidgetStatus.DEFAULT );
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

		if ( (Integer) circleMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) circleMap.get( "totalCount" ) );
			return circleFeature.getCircleSearch().printJsonOutput( responseMap, (List<Circle>) circleMap.get( "circles" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			return responseMap;
		}
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
			final HttpServletResponse response) throws InterruptedException, IOException, ExecutionException
	{
		return circleFeature.getCircleDetail().getCircleDetailById( id );
	}
}