package br.com.anteros.nosql.spring.web.converter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import br.com.anteros.nosql.persistence.session.query.filter.AndExpression;
import br.com.anteros.nosql.persistence.session.query.filter.BetweenExpression;
import br.com.anteros.nosql.persistence.session.query.filter.FieldExpression;
import br.com.anteros.nosql.persistence.session.query.filter.InExpression;
import br.com.anteros.nosql.persistence.session.query.filter.OperationExpression;
import br.com.anteros.nosql.persistence.session.query.filter.OrExpression;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="type")
@JsonSubTypes({
      @JsonSubTypes.Type(value=AndExpression.class, name="AND"),
      @JsonSubTypes.Type(value=BetweenExpression.class, name="BETWEEN"),
      @JsonSubTypes.Type(value=InExpression.class, name="IN"),
      @JsonSubTypes.Type(value=OrExpression.class, name="OR"),
      @JsonSubTypes.Type(value=OperationExpression.class, name="OP"),
      @JsonSubTypes.Type(value=FieldExpression.class, name="FIELD")
  }) 
public abstract class NoSQLJacksonBaseMixin {


}
