package br.com.bluesoft.bee.model.rule

class Rule {
    List<DataType> dataTypeOut = []
    Map<String, Map<String, String>> columnDefaultOut = [:]
    Map<String, String> checkConditionIn = [:]

    List<DataType> dataTypeIn = []
    Map<String, Map<String, String>> columnDefaultIn = [:]
    Map<String, String> checkConditionOut = [:]
}
