package br.com.bluesoft.bee.database.reader;

import spock.lang.Specification

public class TableDataReaderTest extends Specification {

	def "buildQuery deve gerar uma sql"() {
		given:
		def table = [
			name: "teste",
			columns : [
				teste1: [name: "teste1"],
				teste2: [name: "teste2"]
			]
		]

		when:
		def result = new TableDataReader(null).buildQuery(table)

		then:
		result == "select teste1,teste2 from teste"
	}

	def "buildQuery deve gerar uma sql com ordem"() {
		given:
		def table = [
			name: "teste",
			columns : [
				teste1: [name: "teste1"],
				teste2: [name: "teste2"]
			],
			constraints: [
				"pk_teste": [
					name: "pk_teste",
					type: "P",
					columns: ["teste1", "teste2"]]
			]
		]

		when:
		def result = new TableDataReader(null).buildQuery(table)

		then:
		result == "select teste1,teste2 from teste order by teste1,teste2"
	}

	def "getData should return the array with data"() {
		given:
		def table = [
			name: "teste",
			columns : [
				teste1: [name: "teste1"],
				teste2: [name: "teste2"]
			],
			constraints: [
				"pk_teste": [
					name: "pk_teste",
					type: "P",
					columns: ["teste1", "teste2"]]
			]
		]

		def data = [
			[ teste1: "xx1 ", teste2: "yy1"],
			[ teste1: "xx2", teste2: "yy2 "]
		]

		def sql = [
					eachRow: { query, closure ->
						data.each(closure)
					}
				]

		when:
		def result = new TableDataReader(sql).getData(table)

		then:
		result == [
			["xx1", "yy1"],
			["xx2", "yy2"]
		]
	}
}
