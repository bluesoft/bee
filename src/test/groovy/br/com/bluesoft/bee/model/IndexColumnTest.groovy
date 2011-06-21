package br.com.bluesoft.bee.model

import spock.lang.Specification;


class IndexColumnTest extends Specification {
	
	def "the to string to return the name plus the asc/desc info"() {
		
		expect:
		column.toString() == string
		
		where:
		column 											| string
		new IndexColumn(name:'person',descend:false)	| 'person asc'
		new IndexColumn(name:'person',descend:true)		| 'person desc'
		new IndexColumn(name:'full_name')				| 'full_name asc'
		new IndexColumn(name:'full_name',descend:true)	| 'full_name desc'
	}
}
