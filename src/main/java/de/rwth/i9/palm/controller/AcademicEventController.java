package de.rwth.i9.palm.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.ParseException;
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

import de.rwth.i9.palm.datasetcollect.service.DblpEventCollection;
import de.rwth.i9.palm.feature.academicevent.AcademicEventFeature;
import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Event;
import de.rwth.i9.palm.model.EventGroup;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.UserEventGroupBookmark;
import de.rwth.i9.palm.model.UserWidget;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetStatus;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@RequestMapping( value = "/venue" )
public class AcademicEventController
{
	private static final String LINK_NAME = "venue";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private AcademicEventFeature academicEventFeature;

	@Autowired
	private SecurityService securityService;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView eventPage( 
			@RequestParam( value = "id", required = false ) final String id,
			@RequestParam( value = "eventId", required = false ) final String eventId, 
			@RequestParam( value = "name", required = false ) String name,
			@RequestParam( value = "type", required = false ) final String type,
			@RequestParam( value = "abbr", required = false ) String notation, 
			@RequestParam( value = "year", required = false ) final String year,
			@RequestParam( value = "volume", required = false ) final String volume,
			@RequestParam( value = "publicationId", required = false ) final String publicationId,
			@RequestParam( value = "add", required = false ) final String add,
			final HttpServletResponse response) throws InterruptedException
	{
		// set model and view
		ModelAndView model = TemplateHelper.createViewWithLink( "conference", LINK_NAME );

		List<Widget> widgets = new ArrayList<Widget>();

		User user = securityService.getUser();

		if ( user != null )
		{
			List<UserWidget> userWidgets = persistenceStrategy.getUserWidgetDAO().getWidget( user, WidgetType.CONFERENCE, WidgetStatus.ACTIVE );
			for ( UserWidget userWidget : userWidgets )
			{
				Widget widget = userWidget.getWidget();
				widget.setColor( userWidget.getWidgetColor() );
				widget.setWidgetHeight( userWidget.getWidgetHeight() );
				widget.setWidgetWidth( userWidget.getWidgetWidth() );
				widget.setPosition( userWidget.getPosition() );

				widgets.add( widget );
			}
		}
		else
			widgets = persistenceStrategy.getWidgetDAO().getWidget( WidgetType.CONFERENCE, WidgetStatus.DEFAULT );
		// assign the model
		model.addObject( "widgets", widgets );

		EventGroup eventGroup = null;
		// assign query
		if ( id != null )
		{
			model.addObject( "targetId", id );
			eventGroup = persistenceStrategy.getEventGroupDAO().getById( id );
		}
		else
		{
			// get event group id
			if ( eventId != null )
			{
				Event event = persistenceStrategy.getEventDAO().getById( eventId );
				if ( event != null && event.getEventGroup() != null )
				{
					model.addObject( "targetId", event.getEventGroup().getId() );
					eventGroup = event.getEventGroup();
				}
			}
		}
		// check whether event group is added or not

		if ( eventId != null )
		{
			model.addObject( "targetEventId", eventId );
			if ( name == null )
			{
				name = eventGroup.getName();
			}
		}
		if ( name != null )
			model.addObject( "targetName", name.replaceAll( "\"", "" ) );
		if ( notation != null )
			model.addObject( "targetNotation", notation );
		if ( type != null )
			model.addObject( "targetType", type );
		if ( year != null )
			model.addObject( "targetYear", year );
		if ( name != null )
			model.addObject( "targetVolume", volume );
		if ( publicationId != null )
			model.addObject( "publicationId", publicationId );
		if ( add != null )
		{
			if ( eventGroup == null || ( eventGroup != null && !eventGroup.isAdded() ) )
				model.addObject( "targetAdd", add );
		}
		return model;
	}

	@SuppressWarnings( "unchecked" )
	@Transactional
	@RequestMapping( value = "/search", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> getConferenceList( 
			@RequestParam( value = "query", required = false ) String query, 
			@RequestParam( value = "abbr", required = false ) String notation, 
			@RequestParam( value = "startPage", required = false ) Integer startPage, 
			@RequestParam( value = "maxresult", required = false ) Integer maxresult,
			@RequestParam( value = "type", required = false ) String type,
			@RequestParam( value = "source", required = false ) String source,
			@RequestParam( value = "persist", required = false ) String persist,
			HttpServletRequest request,
			HttpServletResponse response)
	{
		if ( query == null ) 		query = "";
		if ( startPage == null )	startPage = 0;
		if ( maxresult == null )	maxresult = 50;
		if ( type == null )			type = "all";
		if ( source == null )		source = "internal";
		if ( persist == null )		persist = "no";
		
		// create JSON mapper for response
		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();
		boolean persistResult = false;

		responseMap.put( "query", query );
		responseMap.put( "type", type );
		responseMap.put( "startPage", startPage );
		responseMap.put( "maxresult", maxresult );
		responseMap.put( "source", source );

		if ( !persist.equals( "no" ) )
		{
			responseMap.put( "persist", persist );
			persistResult = true;
		}
		
		Map<String, Object> eventGroupsMap = academicEventFeature.getEventSearch().getEventGroupMapByQuery( query, notation, startPage, maxresult, source, type, persistResult );

		// store in session
		if ( source.equals( "external" ) || source.equals( "all" ) )
		{
			request.getSession().setAttribute( "eventGroups", eventGroupsMap.get( "eventGroups" ) );
			// recheck if session really has been updated
			// (there is a bug in spring session, which makes session is
			// not updated sometimes) - a little work a round
			boolean isSessionUpdated = false;
			while ( !isSessionUpdated )
			{
				Object eventGroups = request.getSession().getAttribute( "eventGroups" );
				if ( eventGroups.equals( eventGroupsMap.get( "eventGroups" ) ) )
					isSessionUpdated = true;
				else
					request.getSession().setAttribute( "eventGroups", eventGroupsMap.get( "eventGroups" ) );
			}
		}
		if ( (Integer) eventGroupsMap.get( "totalCount" ) > 0 )
		{
			responseMap.put( "totalCount", (Integer) eventGroupsMap.get( "totalCount" ) );
			return academicEventFeature.getEventSearch().printJsonOutput( responseMap, (List<EventGroup>) eventGroupsMap.get( "eventGroups" ) );
		}
		else
		{
			responseMap.put( "totalCount", 0 );
			responseMap.put( "count", 0 );
			return responseMap;
		}
	}
	
	@Transactional
	@RequestMapping( value = "/fetchGroup", method = RequestMethod.GET )
	public @ResponseBody Map<String, Object> fetchEventGroupFromDblp( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force, 
			HttpServletRequest request, HttpServletResponse response) throws ParseException, 
			IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException
	{
		@SuppressWarnings( "unchecked" )
		List<EventGroup> sessionEventGroups = null;// (List<EventGroup>) request.getSession().getAttribute( "eventGroups" );

		Map<String, Object> responseMap = academicEventFeature.getEventMining().fetchEventGroupData( id, pid, sessionEventGroups );

		return responseMap;
	}

	@RequestMapping( value = "/fetch", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> fetchEventFromDblp( 
			@RequestParam( value = "id", required = false ) final String id, 
			@RequestParam( value = "pid", required = false ) final String pid, 
			@RequestParam( value = "force", required = false ) final String force,
			final HttpServletResponse response ) throws ParseException, IOException, InterruptedException, ExecutionException, java.text.ParseException, TimeoutException, OAuthSystemException, OAuthProblemException 
	{
		return academicEventFeature.getEventMining().fetchEventData( id, pid, force );
	}
	
	@RequestMapping( value = "/interest", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> researcherInterest( 
			@RequestParam( value = "id", required = false ) final String eventId, 
			@RequestParam( value = "updateResult", required = false ) final String updateResult,
			final HttpServletResponse response ) throws InterruptedException, IOException, ExecutionException, URISyntaxException, ParseException, java.text.ParseException
	{
		boolean isReplaceExistingResult = false;
		if ( updateResult != null && updateResult.equals( "yes" ) )
			isReplaceExistingResult = true;
		return academicEventFeature.getEventInterest().getEventInterestById( eventId, isReplaceExistingResult );
	}

	@RequestMapping( value = "/publicationList", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getPublicationList( 
			@RequestParam( value = "id", required = false ) final String eventId,
			@RequestParam( value = "query", required = false ) final String query,
			@RequestParam( value = "publicationId", required = false ) final String publicationId, 
			final HttpServletResponse response)
	{
		return academicEventFeature.getEventPublication().getPublicationListByEventId( eventId, query, publicationId );
	}

	@RequestMapping( value = "/autocomplete", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody List<Object> getEventAutoComplete( @RequestParam( value = "query", required = false ) final String query, final HttpServletResponse response)
	{
		return DblpEventCollection.getEventFromDBLPSearch( query, "all", null );
	}

	/**
	 * Get the basic information (publication type, language, etc) from a
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
	@RequestMapping( value = "/basicinformation", method = RequestMethod.GET )
	@Transactional
	public @ResponseBody Map<String, Object> getAcademicEventBasicInformation( 
			@RequestParam( value = "id", required = false ) final String id,
			@RequestParam( value = "type", required = false ) String type, 
			@RequestParam( value = "uri", required = false ) final String uri, 
			final HttpServletResponse response)
	{
		if( type == null ) type="event";
		if ( type.equals( "event" ) )
			return academicEventFeature.getEventBasicStatistic().getEventBasicStatisticById( id );
		else
		{
			Map<String, Object> responseMap = academicEventFeature.getEventBasicStatistic().getEventGroupBasicStatisticById( id );
			// check whether eventGroup is already booked or not
			User user = securityService.getUser();
			if ( user != null )
			{
				EventGroup eventGroup = persistenceStrategy.getEventGroupDAO().getById( id );
				if ( eventGroup == null )
					return responseMap;

				UserEventGroupBookmark upb = persistenceStrategy.getUserEventGroupBookmarkDAO().getByUserAndEventGroup( user, eventGroup );
				if ( upb != null )
					responseMap.put( "booked", true );
				else
					responseMap.put( "booked", false );
			}
			return responseMap;
		}
	}

}