/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package br.com.anteros.nosql.spring.web.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.core.utils.IOUtils;
import br.com.anteros.nosql.persistence.session.NoSQLSession;
import br.com.anteros.nosql.persistence.session.NoSQLSessionFactory;
import br.com.anteros.nosql.spring.transaction.AnterosNoSQLSessionHolder;
import br.com.anteros.nosql.spring.transaction.NoSQLSessionFactoryUtils;

/**
 * 
 * @author Edson Martins edsonmartins2005@gmail.com
 *
 */
public class OpenNoSQLSessionInViewFilter extends OncePerRequestFilter {

	private static final Log LOGGER = LogFactory.getLog(OpenNoSQLSessionInViewFilter.class);

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactoryNoSQL";

	private static Logger LOG = LoggerProvider.getInstance().getLogger(OpenNoSQLSessionInViewFilter.class);

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;

	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

//        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
//        response.setHeader("Access-Control-Max-Age", "3600");
//        response.setHeader("Access-Control-Allow-Credentials","true");
//        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, authorization");

//        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//            response.setStatus(HttpServletResponse.SC_OK);
//        } else {
        	NoSQLSessionFactory sessionFactory = lookupSessionFactory(request);
    		boolean participate = false;

    		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
    		String key = getAlreadyFilteredAttributeName();

    		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
    			participate = true;
    		} else {
    			boolean isFirstRequest = !isAsyncDispatch(request);
    			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
    				LOGGER.debug("Opening single Anteros NoSQLSession in OpenNoSQLSessionInViewFilter");
    				NoSQLSession session = sessionFactory.openSession();
    				AnterosNoSQLSessionHolder sessionHolder = new AnterosNoSQLSessionHolder(session);
    				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

    				AsyncNoSQLRequestInterceptor interceptor = new AsyncNoSQLRequestInterceptor(sessionFactory,
    						sessionHolder);
    				asyncManager.registerCallableInterceptor(key, interceptor);
    				asyncManager.registerDeferredResultInterceptor(key, interceptor);
    			}
    		}

    		try {
    			LOG.debug("Before execute doFilter");
    			filterChain.doFilter(request, response);

    			Collection<String> hds = response.getHeaderNames();
    			LOG.debug("After execute doFilter");
    		} finally {
    			if (!participate) {
    				AnterosNoSQLSessionHolder sessionHolder = (AnterosNoSQLSessionHolder) TransactionSynchronizationManager
    						.unbindResource(sessionFactory);
    				if (!isAsyncStarted(request)) {
    					LOGGER.debug("Closing single Anteros NoSQLSession in OpenNoSQLSessionInViewFilter");
    					closeSession(sessionHolder.getSession(), sessionFactory);
    				}
    			}
    		}
//        }		
	}

	private static void printRequest(final HttpServletRequest httpServletRequest) {
		if (httpServletRequest == null) {
			return;
		}
		LOGGER.info("----------------------------------------");
		LOGGER.info("W4 HttpServletRequest");
		LOGGER.info("\tRequestURL : " + httpServletRequest.getRequestURL());
		LOGGER.info("\tRequestURI : " + httpServletRequest.getRequestURI());
		LOGGER.info("\tScheme : " + httpServletRequest.getScheme());
		LOGGER.info("\tAuthType : " + httpServletRequest.getAuthType());
		LOGGER.info("\tEncoding : " + httpServletRequest.getCharacterEncoding());
		LOGGER.info("\tContentLength : " + httpServletRequest.getContentLength());
		LOGGER.info("\tContentType : " + httpServletRequest.getContentType());
		LOGGER.info("\tContextPath : " + httpServletRequest.getContextPath());
		LOGGER.info("\tMethod : " + httpServletRequest.getMethod());
		LOGGER.info("\tPathInfo : " + httpServletRequest.getPathInfo());
		LOGGER.info("\tProtocol : " + httpServletRequest.getProtocol());
		LOGGER.info("\tQuery : " + httpServletRequest.getQueryString());
		LOGGER.info("\tRemoteAddr : " + httpServletRequest.getRemoteAddr());
		LOGGER.info("\tRemoteHost : " + httpServletRequest.getRemoteHost());
		LOGGER.info("\tRemotePort : " + httpServletRequest.getRemotePort());
		LOGGER.info("\tRemoteUser : " + httpServletRequest.getRemoteUser());
		LOGGER.info("\tSessionID : " + httpServletRequest.getRequestedSessionId());
		LOGGER.info("\tServerName : " + httpServletRequest.getServerName());
		LOGGER.info("\tServerPort : " + httpServletRequest.getServerPort());
		LOGGER.info("\tServletPath : " + httpServletRequest.getServletPath());
		LOGGER.info("");
		LOGGER.info("\tCookies");
		int i = 0;
		if (httpServletRequest.getCookies() != null) {
			for (final Cookie cookie : httpServletRequest.getCookies()) {
				LOGGER.info("\tCookie[" + i + "].name=" + cookie.getName());
				LOGGER.info("\tCookie[" + i + "].comment=" + cookie.getComment());
				LOGGER.info("\tCookie[" + i + "].domain=" + cookie.getDomain());
				LOGGER.info("\tCookie[" + i + "].maxAge=" + cookie.getMaxAge());
				LOGGER.info("\tCookie[" + i + "].path=" + cookie.getPath());
				LOGGER.info("\tCookie[" + i + "].secured=" + cookie.getSecure());
				LOGGER.info("\tCookie[" + i + "].value=" + cookie.getValue());
				LOGGER.info("\tCookie[" + i + "].version=" + cookie.getVersion());
				i++;
			}
		}
		LOGGER.info("\tDispatcherType : " + httpServletRequest.getDispatcherType());
		LOGGER.info("");
		LOGGER.info("\tHeaders");
		int j = 0;
		final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			final String header = httpServletRequest.getHeader(headerName);
			LOGGER.info("\tHeader[" + j + "].name={}" + headerName);
			LOGGER.info("\tHeader[" + j + "].value=" + header);
			j++;
		}
		LOGGER.info("\tLocalAddr : " + httpServletRequest.getLocalAddr());
		LOGGER.info("\tLocale : " + httpServletRequest.getLocale());
		LOGGER.info("\tLocalPort : " + httpServletRequest.getLocalPort());
		LOGGER.info("");
		LOGGER.info("\tParameters");
		int k = 0;
		final Enumeration<String> parameterNames = httpServletRequest.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			final String paramName = parameterNames.nextElement();
			final String paramValue = httpServletRequest.getParameter(paramName);
			LOGGER.info("\tParam[" + k + "].name=" + paramName);
			LOGGER.info("\tParam[" + k + "].value=" + paramValue);
			k++;
		}
		LOGGER.info("");
		LOGGER.info("\tParts");
		int l = 0;
		try {
			for (final Object part : httpServletRequest.getParts()) {
				LOGGER.info("\tParts[" + l + "].class=" + part != null ? part.getClass() : "");
				LOGGER.info("\tParts[" + l + "].value=" + part != null ? part.toString() : "");
				l++;
			}
		} catch (final Exception e) {
			LOGGER.error("Exception e", e);
		}
		printSession(httpServletRequest.getSession());
		printUser(httpServletRequest.getUserPrincipal());
		try {
			LOGGER.info("Request Body : "
					+ IOUtils.toString(httpServletRequest.getInputStream(), httpServletRequest.getCharacterEncoding()));
			LOGGER.info("Request Object : " + new ObjectInputStream(httpServletRequest.getInputStream()).readObject());
		} catch (final Exception e) {
			LOGGER.debug("Exception e", e);
		}
		LOGGER.info("----------------------------------------");
	}

	private static void printSession(final HttpSession session) {
		LOGGER.info("-");
		if (session == null) {
			LOGGER.error("No session");
			return;
		}
		LOGGER.info("\tSession Attributes");
		LOGGER.info("\tSession.id:  " + session.getId());
		LOGGER.info("\tSession.creationTime:  " + session.getCreationTime());
		LOGGER.info("\tSession.lastAccessTime:  " + session.getLastAccessedTime());
		LOGGER.info("\tSession.maxInactiveInterval:  " + session.getMaxInactiveInterval());
		int k = 0;
		final Enumeration<String> attributeNames = session.getAttributeNames();
		while (attributeNames.hasMoreElements()) {
			final String paramName = attributeNames.nextElement();
			final Object paramValue = session.getAttribute(paramName);
			LOGGER.info("\tSession Attribute[" + k + "].name=" + paramName);
			if (paramValue.getClass() != null) {
				LOGGER.info("\tSession Attribute[\"+k+\"].class={}" + paramValue.getClass());
			}
			LOGGER.info("\tSession Attribute[\"+k+\"].value={}" + paramValue);
			k++;
		}
	}

	/**
	 * Prints the user.
	 *
	 * @param userPrincipal the user principal
	 */
	private static void printUser(final Principal userPrincipal) {
		LOGGER.info("-");
		if (userPrincipal == null) {
			LOGGER.info("User Authentication : none");
			return;
		} else {
			LOGGER.info("User Authentication.name : " + userPrincipal.getName());
			LOGGER.info("User Authentication.class : " + userPrincipal.getClass());
			LOGGER.info("User Authentication.value : " + userPrincipal);
		}
	}

	protected NoSQLSessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	protected NoSQLSessionFactory lookupSessionFactory() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					"Using NoSQLSessionFactory '" + getSessionFactoryBeanName() + "' for OpenNoSQLSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryBeanName(), NoSQLSessionFactory.class);
	}

	protected void closeSession(NoSQLSession session, NoSQLSessionFactory sessionFactory) {
		NoSQLSessionFactoryUtils.closeSession(session);
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		if (asyncManager.getCallableInterceptor(key) == null) {
			return false;
		}
		((AsyncNoSQLRequestInterceptor) asyncManager.getCallableInterceptor(key)).bindSession();
		return true;
	}

}
