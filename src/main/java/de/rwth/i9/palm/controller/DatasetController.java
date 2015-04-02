package de.rwth.i9.palm.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.rwth.i9.palm.analytics.api.IAnalytics;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

@Controller
@RequestMapping( value = "/dataset" )
public class DatasetController
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	@Autowired
	private IAnalytics analytics;

	@RequestMapping( method = RequestMethod.GET )
	@Transactional
	public ModelAndView landing( @RequestParam( value = "sessionid", required = false ) final String sessionId, final HttpServletResponse response ) throws InterruptedException
	{
		ModelAndView model = new ModelAndView( "dataset", "link", "dataset" );

		if ( sessionId != null && sessionId.equals( "0" ) )
			response.setHeader( "SESSION_INVALID", "yes" );

		// Publication pub = new Publication();
		// pub.setAbstractOriginal( "something" );
		// persistenceStrategy.getPublicationDAO().persist( pub );

		System.out.println( analytics.getCValueAlgorithm().test( "cvaluetest" ) );

		return model;
	}

}