package br.com.bluesoft.bee.model

import groovy.transform.AutoClone

@AutoClone
class MViewIndexColumn {
    String name
    Boolean descend = false

    public String toString() {
        return name + ' ' + (descend ? 'desc' : 'asc')
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((descend == null) ? 0 : descend.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    boolean equals(other) {
        this.name == other.name && this.descend == other.descend
    }
}
