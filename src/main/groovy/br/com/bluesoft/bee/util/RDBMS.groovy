package br.com.bluesoft.bee.util;

public enum RDBMS {

    ORACLE, MYSQL, POSTGRES

    static RDBMS getByName(String name) {
        return RDBMS.values().find {it.name().toLowerCase() == name }
    }
}
