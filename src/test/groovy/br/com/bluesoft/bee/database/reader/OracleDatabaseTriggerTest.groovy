package br.com.bluesoft.bee.database.reader

import groovy.sql.Sql
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import spock.lang.Specification

import static org.junit.Assert.assertEquals

class OracleDatabaseTriggerTest extends Specification {

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

        final def sql = Mockito.mock(Sql)
        Mockito.when(sql.rows(Mockito.any(String))).thenReturn(triggers)
        reader = new OracleDatabaseReader(sql)
    }

    def 'it should fill the triggers'() {
        expect:
        final def triggers = reader.getTriggers()
        triggers.size() == 2
    }

    def 'it should fill the package name, text and body'() {
        given:
        final def triggers = reader.getTriggers(null)
        def trigger = triggers['trigger1']

        expect:
        'trigger1' == trigger.name
        'line11\nline12\nline13\n' == trigger.text_oracle
    }
}
