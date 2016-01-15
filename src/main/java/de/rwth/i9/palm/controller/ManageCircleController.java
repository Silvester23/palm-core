package de.rwth.i9.palm.controller;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.Author;
import de.rwth.i9.palm.model.Circle;
import de.rwth.i9.palm.model.Publication;
import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "circle" )
@RequestMapping( value = "/circle" )
public class ManageCircleController
{
	private static final String LINK_NAME = "circle";

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the add circle form together with circle object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.GET )
	public ModelAndView addNewCircle( 
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CIRCLE, "add" );

		// create blank Circle
		Circle circle = new Circle();

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "circle", circle );

		return model;
	}

	/**
	 * Save changes from Add circle detail, via Spring binding
	 * 
	 * @param extractionServiceListWrapper
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/add", method = RequestMethod.POST )
	public @ResponseBody Map<String, Object> saveNewCircle( 
			@ModelAttribute( "circle" ) Circle circle,
			@RequestParam( value = "circleResearcher") final String circleResearcherIds,
			@RequestParam( value = "circlePublication") final String circlePublicationIds,
			final HttpServletResponse response) throws InterruptedException
	{
		if ( circle == null )
			return Collections.emptyMap();

		Map<String, Object> responseMap = new LinkedHashMap<String, Object>();

		if ( !circleResearcherIds.equals( "" ) )
		{
			// split by underscore
			String[] idsArray = circleResearcherIds.split( "_" );

			for ( String researcherId : idsArray )
			{
				Author author = persistenceStrategy.getAuthorDAO().getById( researcherId );
				if ( author != null )
					circle.addAuthor( author );
			}
		}

		if ( !circlePublicationIds.equals( "" ) )
		{
			// split by underscore
			String[] idsArray = circlePublicationIds.split( "_" );

			for ( String researcherId : idsArray )
			{
				Publication publication = persistenceStrategy.getPublicationDAO().getById( researcherId );
				if ( publication != null )
					circle.addPublication( publication );
			}
		}

		// add creator from securityService
		User user = securityService.getUser();

		if ( user == null )
		{
			responseMap.put( "status", "error" );
			responseMap.put( "errorMessage", "user not found" );
			return responseMap;
		}
		circle.setCreator( user );

		// add date
		// get current timestamp
		java.util.Date date = new java.util.Date();
		Timestamp currentTimestamp = new Timestamp( date.getTime() );
		circle.setCreationDate( currentTimestamp );

		persistenceStrategy.getCircleDAO().persist( circle );

		responseMap.put( "status", "ok" );

		// put back some of the information
		Map<String, String> circleMap = new LinkedHashMap<String, String>();
		circleMap.put( "id", circle.getId() );

		responseMap.put( "circle", circleMap );

		return responseMap;
	}

	/**
	 * Load the add circle form together with circle object
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( value = "/edit", method = RequestMethod.GET )
	public ModelAndView editCircle( 
			@RequestParam( value = "id") final String circleId,
			final HttpServletResponse response) throws InterruptedException
	{
		ModelAndView model = null;

		if ( securityService.getUser() == null )
		{
			model = TemplateHelper.createViewWithLink( "401", "error" );
			return model;
		}

		model = TemplateHelper.createViewWithLink( "dialogIframeLayout", LINK_NAME );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.CIRCLE, "edit" );

		// create blank Circle
		Circle circle = persistenceStrategy.getCircleDAO().getById( circleId );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "circle", circle );

		return model;
	}
}