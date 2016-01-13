package de.rwth.i9.palm.controller.user;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.model.SessionDataSet;
import de.rwth.i9.palm.model.Widget;
import de.rwth.i9.palm.model.WidgetType;
import de.rwth.i9.palm.persistence.PersistenceStrategy;
import de.rwth.i9.palm.service.ApplicationContextService;
import de.rwth.i9.palm.service.ApplicationService;
import de.rwth.i9.palm.service.SecurityService;

@Controller
@SessionAttributes( "sourceListWrapper" )
@RequestMapping( value = "/user/publication" )
public class UserPublicationController
{
	private static final String LINK_NAME = "user";

	@Autowired
	private ApplicationContextService appService;

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private SecurityService securityService;

	/**
	 * Load the source detail form
	 * 
	 * @param sessionId
	 * @param response
	 * @return
	 * @throws InterruptedException
	 */
	@Transactional
	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView getSources( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response) throws InterruptedException
	{
		// get current session object
		SessionDataSet sessionDataSet = this.appService.getCurrentSessionDataSet();

		// set model and view
		ModelAndView model = TemplateHelper.createViewWithSessionDataSet( "widgetLayoutAjax", LINK_NAME, sessionDataSet );
		List<Widget> widgets = persistenceStrategy.getWidgetDAO().getActiveWidgetByWidgetTypeAndGroup( WidgetType.USER, "publication" );

		// assign the model
		model.addObject( "widgets", widgets );
		model.addObject( "user", securityService.getUser() );

		return model;
	}

}