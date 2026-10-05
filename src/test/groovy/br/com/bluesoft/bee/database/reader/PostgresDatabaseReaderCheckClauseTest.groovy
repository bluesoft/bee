package br.com.bluesoft.bee.database.reader

import org.junit.Test

import static org.junit.Assert.assertEquals

class PostgresDatabaseReaderCheckClauseTest {

    static final def STABLE = "(tipo IS NULL) OR ((tipo)::text = ANY (ARRAY[('PRINCIPAL'::character varying)::text, ('CONTINGENCIA'::character varying)::text]))"

    @Test
    void 'it should rewrite the array cast to the form postgres keeps after recreating the constraint'() {
        def original = "(tipo IS NULL) OR ((tipo)::text = ANY ((ARRAY['PRINCIPAL'::character varying, 'CONTINGENCIA'::character varying])::text[]))"
        assertEquals(STABLE, PostgresDatabaseReader.normalizeCheckClause(original))
    }

    @Test
    void 'it should keep the stable form untouched'() {
        assertEquals(STABLE, PostgresDatabaseReader.normalizeCheckClause(STABLE))
    }

    @Test
    void 'it should handle commas and quotes inside the values'() {
        def original = "(tipo)::text = ANY ((ARRAY['A, B'::character varying, 'D''E'::character varying])::text[])"
        def expected = "(tipo)::text = ANY (ARRAY[('A, B'::character varying)::text, ('D''E'::character varying)::text])"
        assertEquals(expected, PostgresDatabaseReader.normalizeCheckClause(original))
    }

    @Test
    void 'it should keep other conditions untouched'() {
        assertEquals('codigo_complementar > 0', PostgresDatabaseReader.normalizeCheckClause('codigo_complementar > 0'))
    }
}
