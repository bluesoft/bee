package br.com.bluesoft.bee.database.reader

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class MySqlDatabaseReaderProcedureTest {

    def reader

    @Before
    void 'set up'() {
        def procedures = [
                [name: 'PROCEDURE_A'],
                [name: 'PROCEDURE_B']
        ]
        def bodies = [
                [name: 'PROCEDURE_A', text: 'AAA1'],
                [name: 'PROCEDURE_A', text: 'AAA1'],
                [name: 'PROCEDURE_A', text: 'AAA1'],
                [name: 'PROCEDURE_B', text: 'bbb1'],
                [name: 'PROCEDURE_B', text: 'bbb2']
        ]
        def sql = [rows: { query, schema ->
            switch (query) {
                case MySqlDatabaseReader.PROCEDURES_NAME_QUERY: return procedures; break;
                case MySqlDatabaseReader.PROCEDURES_BODY_QUERY: return bodies; break;
            }
        }]
        reader = new MySqlDatabaseReader(sql, 'test')
    }

    @Test
    void 'it should fill the procedures'() {
        def procedures = reader.getProcedures()
        assertEquals 2, procedures.size()
    }

    @Test
    void 'it should fill the procedure name'() {
        def procedures = reader.getProcedures()
        assertEquals 'PROCEDURE_A', procedures['PROCEDURE_A'].name
        assertEquals 'PROCEDURE_B', procedures['PROCEDURE_B'].name
    }
}
