package br.com.bluesoft.bee.database.reader

import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertEquals

class PostgresDatabaseReaderTableTest {

    def reader

    @Before
    void 'set up'() {
        def tableRows = [
                [table_name: 'resumo_crescimento_loja', temporary: 'N']
        ]

        def columns = createColumns()
        def indexes = createIndexes()
        def indexesColumns = createIndexesColumns()

        def sql = [rows: { query ->
            switch (query) {
                case PostgresDatabaseReader.TABLES_QUERY: return tableRows; break;
                case PostgresDatabaseReader.TABLES_COLUMNS_QUERY: return columns; break;
                case PostgresDatabaseReader.INDEXES_QUERY: return indexes; break;
                case PostgresDatabaseReader.INDEXES_COLUMNS_QUERY: return indexesColumns; break;
            }
        }]

        reader = new PostgresDatabaseReader(sql)
    }

    @Test
    @Ignore
    void 'it should fill the tables'() {
        def tables = reader.getTables(null)
        assertEquals(2, tables.size())
    }


    def createColumns() {
        def data = [table_name  : 'resumo_crescimento_loja',
                    column_name : 'data',
                    data_type   : "date",
                    nullable    : "YES",
                    data_size   : null,
                    data_scale  : null,
                    data_default: null
        ]

        def loja_key = [table_name  : 'resumo_crescimento_loja',
                        column_name : 'loja_key',
                        data_type   : "character",
                        nullable    : "YES",
                        data_size   : null,
                        data_scale  : null,
                        data_default: null
        ]
        return [data, loja_key]
    }

    def createIndexes() {
        def indexes = [
                [table_name: 'resumo_crescimento_loja', index_name: 'idx_resumo_crescimento_loja_data', uniqueness: 'f', index_type: 'n', indkey: '1'],
                [table_name: 'resumo_crescimento_loja', index_name: 'idx_resumo_crescimento_loja_loja', uniqueness: 'f', index_type: 'n', indkey: '2']
        ]
    }

    def createIndexesColumns() {

        def idx_resumo_crescimento_loja_data = [
                table_name : 'resumo_crescimento_loja',
                index_name : 'idx_resumo_crescimento_loja_data',
                column_name: 'data',
                attnum     : '1'
        ]

        def idx_resumo_crescimento_loja_loja = [
                table_name : 'resumo_crescimento_loja',
                index_name : 'idx_resumo_crescimento_loja_loja',
                column_name: 'loja_key',
                attnum     : '2'
        ]

        return [
                idx_resumo_crescimento_loja_data,
                idx_resumo_crescimento_loja_loja
        ]
    }
}
