package br.com.bluesoft.bee.testutils

import groovy.sql.Sql
import spock.lang.Specification

class BeeTesterSpecification extends Specification {


    public static final String BEE_TEMP_DIRECTORY = "/tmp/bee"

    void createBeeFolders() {

        File destTablesFolder = new File("${BEE_TEMP_DIRECTORY}/tables")
        destTablesFolder.mkdirs()

        File destViewsFolder = new File("${BEE_TEMP_DIRECTORY}/views")
        destViewsFolder.mkdirs()

        File destProceduressFolder = new File("${BEE_TEMP_DIRECTORY}/procedures")
        destProceduressFolder.mkdirs()

        File destPackagesFolder = new File("${BEE_TEMP_DIRECTORY}/packages")
        destPackagesFolder.mkdirs()

        File destTriggersFolder = new File("${BEE_TEMP_DIRECTORY}/triggers")
        destTriggersFolder.mkdirs()

        File destUserTypesFolder = new File("${BEE_TEMP_DIRECTORY}/usertypes")
        destUserTypesFolder.mkdirs()


        File srcTablesFolder = new File(this.getClass().getResource("/tables").getFile())
        srcTablesFolder.eachFile {
            new File(destTablesFolder, it.getName()) << it.getText()
        }

        File srcViewsFolder = new File(this.getClass().getResource("/views").getFile())
        srcViewsFolder.eachFile {
            new File(destViewsFolder, it.getName()) << it.getText()
        }

        File srcProceduresFolder = new File(this.getClass().getResource("/procedures").getFile())
        srcProceduresFolder.eachFile {
            new File(destProceduressFolder, it.getName()) << it.getText()
        }

        File srcPackagesFolder = new File(this.getClass().getResource("/packages").getFile())
        srcPackagesFolder.eachFile {
            new File(destPackagesFolder, it.getName()) << it.getText()
        }

        File srcTriggersFolder = new File(this.getClass().getResource("/triggers").getFile())
        srcTriggersFolder.eachFile {
            new File(destTriggersFolder, it.getName()) << it.getText()
        }

        File srcUserTypesFolder = new File(this.getClass().getResource("/usertypes").getFile())
        srcUserTypesFolder.eachFile {
            new File(destUserTypesFolder, it.getName()) << it.getText()
        }

        new File("${BEE_TEMP_DIRECTORY}/sequences.bee") <<
        new File(this.getClass().getResource("/sequences.bee").getFile()).getText()
    }

    void mockSqlEachRow(sql, data) {
        sql.eachRow(_, _) >> { String query, Closure closure ->
            for (int i = 0; i < data.size(); i++) {
                closure.call(data[i])
            }
        }
    }

    Sql getSqlMock() {
        Mock(Sql)
    }

    void mockSqlEachRowConditional(sql, queryCondition, data) {
        sql.eachRow(queryCondition as String, _ as Closure) >> { String query, Closure closure ->
            for (int i = 0; i < data.size(); i++) {
                closure.call(data[i])
            }
        }
    }


}
