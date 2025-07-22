package br.com.bluesoft.bee.database.reader

import groovy.sql.Sql
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import static org.junit.Assert.assertEquals

class OracleDatabaseReaderProcedureTest {

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

        final def sql = Mockito.mock(Sql)
        Mockito.when(sql.rows(OracleDatabaseReader.PROCEDURES_NAME_QUERY)).thenReturn(procedures)
        Mockito.when(sql.rows(OracleDatabaseReader.PROCEDURES_BODY_QUERY)).thenReturn(bodies)
        reader = new OracleDatabaseReader(sql)
    }

    @Test
    void 'it should fill the procedures'() {
        def procedures = reader.getProcedures()
        assertEquals 2, procedures.size()
    }

    @Test
    void 'it should fill the procedure name'() {
        def procedures = reader.getProcedures()
        assertEquals 'procedure_a', procedures['procedure_a'].name
        assertEquals 'procedure_b', procedures['procedure_b'].name
    }
}
