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
package br.com.anteros.nosql.spring.web.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.nosql.persistence.session.query.Example;
import br.com.anteros.nosql.persistence.session.query.NoSQLQuery;
import br.com.anteros.nosql.persistence.session.query.Page;
import br.com.anteros.nosql.persistence.session.query.Sort;
import br.com.anteros.nosql.persistence.session.query.filter.AnterosFilterDsl;
import br.com.anteros.nosql.persistence.session.query.filter.DefaultFilterBuilder;
import br.com.anteros.nosql.persistence.session.query.filter.Filter;
import br.com.anteros.nosql.persistence.session.repository.PageRequest;
import br.com.anteros.nosql.persistence.session.service.NoSQLService;

/**
 * Classe base para uso de serviços REST de persistência usando Anteros and NoSQL.
 * 
 * @author Edson Martins edsonmartins2005@gmail.com
 *
 * @param <T> Tipo
 * @param <ID> ID
 */
@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
@SuppressWarnings("unchecked")
public abstract class AbstractNoSQLResourceRest<T, ID> {

	protected static Logger log = LoggerProvider.getInstance().getLogger(AbstractNoSQLResourceRest.class.getName());

	/**
	 * Método abstrato que irá fornecer a classe de serviço para ser usada no
	 * resource.
	 * 
	 * @return
	 */
	public abstract NoSQLService<T, ID> getService();

	@RequestMapping(value = "/", method = { RequestMethod.POST, RequestMethod.PUT })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
	public <S extends T> S save(@RequestBody S entity) throws Exception {
		return getService().save(entity);
	}

	@RequestMapping(value = "/", method = { RequestMethod.POST, RequestMethod.PUT })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		return getService().save(entities);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public Optional<T> findById(@PathVariable(value = "id") String id) {
		ID castID = (ID) id;
		return getService().findById(castID);
	}

	@RequestMapping(value = "/exists/{id}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public boolean existsById(@PathVariable String id) {
		ID castID = (ID) id;
		return getService().existsById(castID);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/findAll", params = { "page", "size" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public Page<T> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
		PageRequest pageRequest = new PageRequest(page, size);
		return getService().findAll(pageRequest);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/findWithPage", params = { "rsql", "page", "size" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public Page<T> findAll(@RequestParam("rsql") String rsql, @RequestParam("page") int page,
			@RequestParam("size") int size) {
		PageRequest pageRequest = new PageRequest(page, size);
		NoSQLQuery<?> query = getService().parseRsql(rsql).with(pageRequest);
		return getService().findWithPage(query, new PageRequest(page, size));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/findAll")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public Iterable<T> findById(@RequestParam(required = true) List<String> ids) {
		List<ID> newIds = new ArrayList<ID>();
		for (String id : ids) {
			ID castID = (ID) id;
			newIds.add(castID);
		}
		return getService().findById(newIds);
	}
	
	@RequestMapping(value = "/findWithFilter", params = { "page", "size" }, method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public Page<T> find(@RequestBody Filter filter, @RequestParam(value = "page", required = true) int page,
			@RequestParam(value = "size", required = true) int size) throws Exception {
		PageRequest pageRequest = new PageRequest(page, size);

		DefaultFilterBuilder builder = AnterosFilterDsl.getFilterBuilder();

		String sort = builder.toSortNoSql(getService().getSession().getDialect(), filter);
		
		String command = builder.toNoSql(getService().getSession().getDialect(),filter);

		throw new RuntimeException("Falta implementar serialização de Filter para NoSQL(Mongo)");
	}

	@RequestMapping(value = "/count", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public long count() {
		return getService().count();
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
	public void removeById(@PathVariable(value = "id") String id) {
		ID castID = (ID) id;
		getService().removeById(castID);
	}

	@RequestMapping(value = "/", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = false)
	public void remove(@RequestParam(required = true) List<String> ids) {
		for (String id : ids) {
			ID castID = (ID) id;
			getService().removeById(castID);
		}
	}

	@RequestMapping(value = "/findOneByExample", method = { RequestMethod.POST })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> Optional<S> findOne(@RequestBody S example) {
		return getService().findOne(Example.of(example));
	}

	@RequestMapping(value = "/findByExample", method = { RequestMethod.POST })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> Iterable<S> find(@RequestBody S example) {
		return getService().find(Example.of(example));
	}

	@RequestMapping(value = "/findByExample", method = { RequestMethod.POST }, params = { "sort" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> Iterable<S> find(@RequestBody S example, @PathVariable(value = "sort") String sort) {
		return getService().find(Example.of(example), Sort.parse(sort));
	}

	@RequestMapping(value = "/findByExample", method = { RequestMethod.POST }, params = { "sort", "page", "size" })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> Page<S> find(@RequestBody S example, @RequestParam(value = "page") int page,
			@RequestParam(value = "size") int size) {
		return getService().find(Example.of(example), new PageRequest(page, size));
	}

	@RequestMapping(value = "/countByExample", method = { RequestMethod.POST })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> long count(@RequestBody S example) {
		return getService().count(Example.of(example));
	}

	@RequestMapping(value = "/existsByExample", method = { RequestMethod.POST })
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED, readOnly = true)
	public <S extends T> boolean exists(@RequestBody S example) {
		return getService().exists(Example.of(example));
	}

}
