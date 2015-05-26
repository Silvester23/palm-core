package de.rwth.i9.palm.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import de.rwth.i9.palm.model.User;
import de.rwth.i9.palm.persistence.PersistenceStrategy;

/**
 * Service class which should provide methods for security affairs. The
 * authentification itself is managed by Spring.
 * 
 */
@Service
public class SecurityService
{

	@Autowired
	private PersistenceStrategy persistenceStrategy;

	/**
	 * @return
	 */
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		Authentication auth = getAuthentication();

		if ( auth == null )
			return Collections.emptyList();

		return auth.getAuthorities();
	}

	/**
	 * Returns the Authentication-object which is managed by the Spring-context.
	 * 
	 * @return
	 */
	public Authentication getAuthentication()
	{
		SecurityContext securityContext = SecurityContextHolder.getContext();

		if ( securityContext == null )
			return null;

		return securityContext.getAuthentication();
	}

	/**
	 * Returns the username of the current Spring-managed
	 * Authentification-object. The Authentification-object is retrieved via
	 * {@link SecurityService.getAuthentification()}.
	 * 
	 * @return
	 */
	public String getUsername()
	{
		Authentication authentication = getAuthentication();

		if ( authentication == null )
			return null;

		return authentication.getName();
	}

	/**
	 * Returns the {@link User}-object of the missy model associated with the
	 * current Spring-logged in User-object by username. The username is
	 * retrieved via {@link SecurityService.getUsername()}.
	 * 
	 * @return
	 */
	public User getUser()
	{
		String username = getUsername();

		User user = persistenceStrategy.getUserDAO().getByUsername( username );

		return user;
	}

	/**
	 * Returns true or false in case the User is authenticated with the
	 * application or not. This is not self-managed, but managed by Spring
	 * itself.
	 * 
	 * @return
	 */
	public boolean isUserAuthenticated()
	{

		// if ( !getAuthentication().isAuthenticated() )
		// return false;

		// return true;
		return false;
	}

	/**
	 * @param functionName
	 * @return
	 */
	public boolean isAuthorizedForFunction( final String functionName )
	{
		return persistenceStrategy.getUserDAO().isAuthorizedForFunction( getUser(), functionName );
	}
}