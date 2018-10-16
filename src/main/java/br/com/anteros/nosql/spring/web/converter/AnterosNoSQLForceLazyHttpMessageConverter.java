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
package br.com.anteros.nosql.spring.web.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import br.com.anteros.nosql.persistence.serialization.jackson.AnterosNoSQLObjectMapper;
import br.com.anteros.nosql.persistence.serialization.jackson.AnterosPersistenceJacksonModule.Feature;
import br.com.anteros.nosql.persistence.session.NoSQLSessionFactory;


/**
 * 
 * @author Edson Martins edsonmartins2005@gmail.com
 *
 */
@Component
public class AnterosNoSQLForceLazyHttpMessageConverter extends MappingJackson2HttpMessageConverter {

	private NoSQLSessionFactory sessionFactory;

	public AnterosNoSQLForceLazyHttpMessageConverter(NoSQLSessionFactory sessionFactory) {
		this.setSessionFactory(sessionFactory);
	}

	@Autowired
	public void setSessionFactory(NoSQLSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		AnterosNoSQLObjectMapper mapper = new AnterosNoSQLObjectMapper(sessionFactory);
		mapper.enable(Feature.FORCE_LAZY_LOADING);
		this.setObjectMapper(mapper);
	}

	public NoSQLSessionFactory getSessionFactory() {
		return sessionFactory;
	}


}
