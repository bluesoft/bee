package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.importer.BeeImporter
import br.com.bluesoft.bee.model.Schema
import br.com.bluesoft.bee.model.TableColumn
import br.com.bluesoft.bee.model.rule.DataType
import br.com.bluesoft.bee.util.RDBMS
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class RulesConverterTest extends Specification {

    @Rule
    public TemporaryFolder temporaryFolder

    BeeImporter importer
    Schema metadata

    def setup() {

        [ "tables", "views", "procedures", "packages", "triggers", "usertypes" ].each {
            File destDir = temporaryFolder.newFolder(it)
            File srcDir = new File(this.getClass().getResource("/${it}").getFile())
            srcDir.eachFile {
                new File(destDir, it.getName()) << it.getText()
            }
        }

        [ "sequences.bee", "rules.json" ].each {
            temporaryFolder.newFile(it) << new File(this.getClass().getResource("/${it}").getFile()).getText()
        }

        importer = new BeeImporter(temporaryFolder.root.path)
        metadata = importer.importMetaData()
        metadata.rdbms = RDBMS.POSTGRES
    }

    def 'it should convert a table'() {
        expect:
        Schema newSchema = new RulesConverter().toSchema(metadata)
        newSchema.tables[table].columns[column].type == type
        newSchema.tables[table].columns[column].size == size
        newSchema.tables[table].columns[column].scale == scale
        newSchema.tables[table].columns[column].defaultValue == defaultValue

        where:
        table    |  column                     |  type       |  size  |  scale  |  defaultValue
        'cof'    |  'codigo_operacao_fiscal'   | 'integer'   |  0     |  0      |  null
        'cof'    |  'basico'                   | 'number'    |  1     |  0      |  null
        'cof'    |  'descricao'                | 'varchar'   |  100   |  0      |  "teste"
        'cof'    |  'cof_nf_str'               | 'varchar'   |  4     |  0      |  "'1'::character varying"
        'pessoa' |  'data_nascimento'          | 'date'      |  7     |  0      |  'current_date'
        'pessoa' |  'fundacao'                 | 'timestamp' |  7     |  0      |  'current_timestamp'

    }

    def testConvertCheckConstraint() {
        expect:
        Schema newSchema = new RulesConverter().toSchema(metadata)
        newSchema.tables[table].constraints[check].searchCondition == result

        where:
        table    | check       |  result
        'cof'    | 'ck_test'   | 'codigo_complementar <> 0'
        'cof'    | 'ck_test2'  | 'codigo_complementar < 0'

    }

    def testFindDataType() {
        given:
        List<DataType> types = [
                new DataType(fromType: "number", fromSize: 2, fromScale: 0),
                new DataType(fromType: "number", fromSize: 2, fromScale: 1),
                new DataType(fromType: "number", fromSize: 2, fromScale: 2),
                new DataType(fromType: "number", fromSize: 3, fromScale: -1),
        ]
        def converter = new RulesConverter()

        expect:
        (converter.findDataType(column, types) != null) == result

        where:
        column                                                |  result
        new TableColumn(type: "number", size: 2, scale: 0) |  true
        new TableColumn(type: "number", size: 2, scale: 1)    |  true
        new TableColumn(type: "number", size: 2, scale: null) |  false
        new TableColumn(type: "number", size: 3, scale: 0)    |  true
        new TableColumn(type: "number", size: 3, scale: 1)    |  true
        new TableColumn(type: "number", size: 3, scale: null) |  true
        new TableColumn(type: "number", size: 4, scale: 1)    |  false

    }
}
