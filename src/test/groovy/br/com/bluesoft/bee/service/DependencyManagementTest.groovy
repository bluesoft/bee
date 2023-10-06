package br.com.bluesoft.bee.service;

import br.com.bluesoft.bee.model.View
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyManagementTest {

    @Test
    void topologicalSorted() {
        View viewSemDepsOrdem1 = new View(name: "viewSemDependenciasParaTesteManterOrdem1")
        View viewSemDepsOrdem2 = new View(name: "viewSemDependenciasParaTesteManterOrdem2")
        View viewSemDepsOrdem3 = new View(name: "viewSemDependenciasParaTesteManterOrdem3")
        View viewSemDeps = new View(name: "viewSemDependencias")
        View viewComDeps = new View(name: "viewComDependencias", dependencies: [ viewSemDeps.name ])
        View viewComDeps2 = new View(name: "viewComDependencias2", dependencies: [ viewComDeps.name ])
        View viewComDeps3 = new View(name: "viewComDependencias3", dependencies: [ viewComDeps.name, viewComDeps2.name ])
        List<View> views = List.of(viewSemDepsOrdem1, viewSemDepsOrdem2, viewSemDepsOrdem3, viewComDeps, viewComDeps3, viewSemDeps, viewComDeps2)

        var sorted = DependencyManagement.topologicalSorted(views)

        assertEquals(List.of(
                viewSemDepsOrdem1,
                viewSemDepsOrdem2,
                viewSemDepsOrdem3,
                viewSemDeps,
                viewComDeps,
                viewComDeps2,
                viewComDeps3
        ), sorted)
    }

    @Test
    void cyclicalDependencyDetection() {
        View viewComDeps = new View(name: "viewComDependencias", dependencies: [])
        View viewComDeps2 = new View(name: "viewComDependencias2", dependencies: [ viewComDeps.name ])
        View viewComDeps3 = new View(name: "viewComDependencias3", dependencies: [ viewComDeps.name, viewComDeps2.name ])
        viewComDeps.dependencies.add(viewComDeps3.name)
        List<View> views = List.of(viewComDeps, viewComDeps3, viewComDeps2)

        var exception = assertThrows(IllegalStateException.class, () -> DependencyManagement.topologicalSorted(views))

        assertEquals("Cyclic dependency detected: viewComDependencias", exception.message)
    }
}