package br.com.bluesoft.bee.database.reader


import spock.lang.Specification

import static org.junit.Assert.assertEquals

class MySqlDatabaseTriggerTest extends Specification {

    def reader

    void setup() {
        def triggers = [
                [name: 'TRIGGER1', text: 'line11\n'],
                [name: 'TRIGGER1', text: 'line12\n'],
                [name: 'TRIGGER1', text: 'line13\n'],
                [name: 'TRIGGER2', text: 'line21\n'],
                [name: 'TRIGGER2', text: 'line22\n'],
                [name: 'TRIGGER2', text: 'line23\n']
        ]
        final def sql = [rows: { query, schema -> return triggers }]
        reader = new MySqlDatabaseReader(sql, 'test')
    }

    def 'it should fill the triggers'() {
        expect:
        final def triggers = reader.getTriggers()
        2 == triggers.size()
    }

    def 'it should fill the package name, text and body'() {
        given:
        final def triggers = reader.getTriggers(null)
        def trigger = triggers['TRIGGER1']

        expect:
        'TRIGGER1' == trigger.name
        'line11\nline12\nline13\n' == trigger.text_mysql
    }
}
