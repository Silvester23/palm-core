package de.rwth.i9.palm.controller.user;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.helper.TemplateHelper;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/user" )
public class UserController
{

	private static final String LINK_NAME = "user";
	
	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@RequestMapping( method = RequestMethod.GET )
	public ModelAndView landing( 
			@RequestParam( value = "page", required = false ) final String page,
			final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = TemplateHelper.createViewWithLink( "user", LINK_NAME );

		if( page != null)
			model.addObject( "activeMenu", page );
		else
			model.addObject( "activeMenu", "profile" );

		return model;
	}

}