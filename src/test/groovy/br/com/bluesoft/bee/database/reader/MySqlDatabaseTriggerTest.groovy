package br.com.bluesoft.bee.database.reader

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class MySqlDatabaseTriggerTest {

    def reader

    @Before
    void 'set up'() {
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

    @Test
    void 'it should fill the triggers'() {
        final def triggers = reader.getTriggers()
        assertEquals 2, triggers.size()
    }

    @Test
    void 'it should fill the package name, text and body'() {
        final def triggers = reader.getTriggers(null)
        def trigger = triggers['TRIGGER1']
        assertEquals('TRIGGER1', trigger.name)
        assertEquals('line11\nline12\nline13\n', trigger.text)
    }
}
