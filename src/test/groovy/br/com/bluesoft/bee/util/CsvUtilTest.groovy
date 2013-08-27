package br.com.bluesoft.bee.util

import spock.lang.*


class CsvUtilTest extends Specification {

	def "read a file"() {
		given:
		def msg
		def file = [
					eachLine: { enc, closure ->
						txt.split("\n").each(closure)
					}
				]

		msg = CsvUtil.read(file)
		expect:
		msg == dados

		where:
		txt						| dados
		'xx,yy\nyy'				|	[['xx', 'yy'], ['yy']]
		'xx\\,yy,zz\nyy'		|	[['xx,yy', 'zz'], ['yy']]
		'xx,,zz\nyy'			|	[['xx', null , 'zz'], ['yy']]
		'xx,yy,'				|	[['xx', 'yy', null]]
		'xx,yy\nyy,zz\\nzz'     |   [['xx', 'yy'], ['yy', 'zz\nzz']]
	}
}
