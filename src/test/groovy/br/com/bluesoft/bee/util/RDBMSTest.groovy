package br.com.bluesoft.bee.util

import spock.lang.Specification

class RDBMSTest extends Specification {

    def 'shoud return the right type by name'() {
        expect:
        println RDBMS.getByName(name)
        RDBMS.getByName(name) == type

        where:
        name << [ "postgres", "oracle", "mysql", "xyz" ]
        type << [ RDBMS.POSTGRES, RDBMS.ORACLE, RDBMS.MYSQL, null]
    }
}
