package br.com.bluesoft.bee.database.reader

import groovy.sql.Sql
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

import static org.junit.Assert.assertEquals

public class OracleDatabaseReaderSequenceTest {

    def reader

    @Before
    void 'set up'() {
        def sequences = [
                [sequence_name: 'SEQ_PESSOA', min_value: 5000],
                [sequence_name: 'SEQ_TIPO_PESSOA', min_value: 0]
        ]

        final def sql = Mockito.mock(Sql)
        Mockito.when(sql.rows(Mockito.any(String))).thenReturn(sequences)
        reader = new OracleDatabaseReader(sql)
    }


    @Test
    void 'it should fill the sequences'() {
        def sequences = reader.getSequences(null)
        assertEquals 2, sequences.size()
    }

    @Test
    void 'it should fill the sequence name'() {
        def sequences = reader.getSequences()
        assertEquals 'seq_pessoa', sequences['seq_pessoa'].name
        assertEquals 'seq_tipo_pessoa', sequences['seq_tipo_pessoa'].name
    }

    @Test
    void 'it should fill the sequence min value'() {
        def sequences = reader.getSequences()
        assertEquals 5000, sequences['seq_pessoa'].minValue
        assertEquals 0, sequences['seq_tipo_pessoa'].minValue
    }
}
