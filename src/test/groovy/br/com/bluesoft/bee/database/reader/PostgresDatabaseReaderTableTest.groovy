package br.com.bluesoft.bee.database.reader

import static org.junit.Assert.*

import org.junit.Before;
import org.junit.Test


class PostgresDatabaseReaderTableTest {

	def reader

	@Before
	void 'set up'(){
		def tableRows = [
			[table_name:'resumo_estoque_em_transito', temporary:'N']
		]

		def columns = createColumns()
		def indexes = createIndexes()

		def sql = [ rows: { query ->
				switch (query) {
					case PostgresDatabaseReader.TABLES_QUERY: return tableRows; break;
					case PostgresDatabaseReader.TABLES_COLUMNS_QUERY: return columns; break;
					case PostgresDatabaseReader.INDEXES_QUERY_9_6: return indexes; break;
				}
			} ]

		reader = new PostgresDatabaseReader(sql)
	}

	@Test
	void  'it should fill the tables'() {
		def tables = reader.getTables(null,'10.6')
		assertEquals(1, tables.size())
	}

	@Test
	void 'it should set fields of column'() {
		def tables = reader.getTables(null,'10.6')
		assertTrue tables['resumo_estoque_em_transito'].columns['data'].nullable
		assertFalse tables['resumo_estoque_em_transito'].columns['loja_key'].nullable
		assertEquals "date", tables['resumo_estoque_em_transito'].columns['data'].type
		assertEquals 5, tables['resumo_estoque_em_transito'].columns['data'].size
		assertEquals 0, tables['resumo_estoque_em_transito'].columns['data'].scale
		assertNull(tables['resumo_estoque_em_transito'].columns['data'].defaultValue)
	}

	@Test
	void 'it should set fields of index'() {
		def tables = reader.getTables(null,'10.6')
		assertEquals "n", tables['resumo_estoque_em_transito'].indexes['idx_ret_calendario_key'].type
		assertFalse tables['resumo_estoque_em_transito'].indexes['idx_ret_calendario_key'].unique
		assertFalse tables['resumo_estoque_em_transito'].indexes['idx_ret_calendario_key'].columns[0].descend

		assertEquals "n", tables['resumo_estoque_em_transito'].indexes['idx_ret_data_recebimento'].type
		assertTrue tables['resumo_estoque_em_transito'].indexes['idx_ret_data_recebimento'].unique
		assertTrue tables['resumo_estoque_em_transito'].indexes['idx_ret_data_recebimento'].columns[0].descend
	}


	def createColumns() {
		def data = [table_name:'resumo_estoque_em_transito',
			column_name:'data',
			data_type:"date",
			nullable: 'YES',
			data_size:5,
			data_scale:null,
			data_default:null
		]

		def loja_key = [table_name:'resumo_estoque_em_transito',
			column_name:'loja_key',
			data_type:"character",
			nullable: 'NO',
			data_size:null,
			data_scale:null,
			data_default:null
		]
		return [data, loja_key]
	}

	def createIndexes() {
		def indexes = [
			[table_name: 'resumo_estoque_em_transito', index_name: 'idx_ret_calendario_key', uniqueness: false, index_type: 'btree', column_name:'calendario_key', descend:'asc'],
			[table_name: 'resumo_estoque_em_transito', index_name: 'idx_ret_data_recebimento', uniqueness: true, index_type: 'btree',column_name:'data_recebimento', descend:'desc']
		]
		return indexes
	}

}
