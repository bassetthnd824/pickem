package com.curleesoft.pickem.filter;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.curleesoft.pickem.bean.SeasonBean;
import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.util.Globals;

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR }, urlPatterns = { "/game/*", "/manager/*" })
public class LoginFilter implements Filter {

	private static final Log log = LogFactory.getLog(LoginFilter.class);

	@Inject
	private UserBean userBean;
	
	@Inject
	private SeasonBean seasonBean;

	/**
	 * Default constructor.
	 */
	public LoginFilter() {}

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		log.debug("In doFilter method of " + this.getClass().getSimpleName());

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		User user = (User) httpRequest.getSession().getAttribute(Globals.ACTIVE_USER);

		if (user == null) {
			log.debug("User is null.  Looking up user in database...");
			String userId = httpRequest.getUserPrincipal().getName();
			boolean isManager = httpRequest.isUserInRole("manager");
			user = userBean.getUserById(userId);
			httpRequest.getSession().setAttribute(Globals.ACTIVE_USER, user);
			httpRequest.getSession().setAttribute("isManager", isManager);
		}

		Season currentSeason = (Season) httpRequest.getSession().getAttribute(Globals.CURRENT_SEASON);

		if (currentSeason == null) {
			log.debug("Current Season is null.  Looking up season in database...");
			currentSeason = seasonBean.getCurrentSeason();
			httpRequest.getSession().setAttribute(Globals.CURRENT_SEASON, currentSeason);
		}

		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

}
