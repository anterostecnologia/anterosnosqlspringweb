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
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	
	private static final Log logger = LogFactory.getLog(OpenNoSQLSessionInViewFilter.class);

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

		NoSQLSessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			participate = true;
		} else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening single Anteros NoSQLSession in OpenNoSQLSessionInViewFilter");
				NoSQLSession session = sessionFactory.openSession();
				AnterosNoSQLSessionHolder sessionHolder = new AnterosNoSQLSessionHolder(session);
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

				AsyncNoSQLRequestInterceptor interceptor = new AsyncNoSQLRequestInterceptor(sessionFactory, sessionHolder);
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
					logger.debug("Closing single Anteros NoSQLSession in OpenNoSQLSessionInViewFilter");
					closeSession(sessionHolder.getSession(), sessionFactory);
				}
			}
		}
	}

	protected NoSQLSessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	protected NoSQLSessionFactory lookupSessionFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug(
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
