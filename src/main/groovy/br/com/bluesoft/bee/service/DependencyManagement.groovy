package br.com.bluesoft.bee.service

import br.com.bluesoft.bee.model.WithDependencies
import br.com.bluesoft.bee.util.RDBMS

class DependencyManagement {

    /**
     * Returns a list of objects sorted topologically by their dependencies using DFS.
     */
    static <T extends WithDependencies> List<T> topologicalSorted(RDBMS rdbms, Collection<T> list) {
        return new TopologicalSort<T>(rdbms, list).sort();
    }

    private static final class TopologicalSort<T extends WithDependencies> {
        private final RDBMS rdbms;
        private final LinkedHashMap<String, T> result;
        private final Collection<T> list;
        private final Map<String, T> byName;
        private final Set<String> visited;

        TopologicalSort(RDBMS rdbms, Collection<T> list) {
            this.rdbms = rdbms;
            this.list = list;
            this.byName = list.collectEntries { [it.name, it] };
            this.result = new LinkedHashMap<String, T>()
            this.visited = new HashSet<>();
        }

        List<T> sort() {
            list.each {
                visit(it)
            }
            return result.values().toList();
        }

        private void visit(T object) {
            if (result.containsKey(object.name)) {
                return
            }
            if (visited.contains(object.name)) {
                throw new IllegalStateException("Cyclic dependency detected: ${object.name}")
            }

            visited.add(object.name)

            object.getDependencies(rdbms)?.each {
                visit(byName[it])
            }

            visited.remove(object.name)
            result.put(object.name, object)
        }
    }

}
