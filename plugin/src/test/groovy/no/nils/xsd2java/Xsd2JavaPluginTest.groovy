package no.nils.xsd2java

import org.junit.Test
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import static org.junit.Assert.assertTrue

class Xsd2JavaPluginTest{

	@Test
    public void canAddXsdTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('xsd2java', type: Xsd2JavaTask)
        assertTrue(task instanceof Xsd2JavaTask)
    }

    @Test
    public void xsd2javaPluginAddsXsd2javaTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'no.nils.xsd2java'

        assertTrue(project.tasks.xsd2java instanceof Xsd2JavaTask)
    }
}