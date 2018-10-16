package br.com.anteros.nosql.spring.web.converter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.anteros.nosql.persistence.session.query.filter.Operator;



public abstract class NoSQLGroupExpressionMixin {

	@JsonIgnore
	public abstract Operator getOperator();

}
