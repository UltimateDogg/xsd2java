package no.nils.xsd2java

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class Xsd2JavaPlugin implements Plugin<Project> {
    public static final String XSD2JAVA = "xsd2java"
	public static final String CLEAN = "deleteGeneratedXsdSources"
	public static final String XSD2JAVA_EXT = "xsd2javaExt"

    public static final DEFAULT_DESTINATION_DIR = "build/generatedsources/src/main/java"

    void apply(Project project) {
        // make sure the project has the java plugin
        project.apply(plugin: 'java')

		Xsd2JavaPluginExtension ext = project.extensions.create(XSD2JAVA_EXT, Xsd2JavaPluginExtension.class)

        Configuration xsd2javaConfiguration = project.configurations.maybeCreate(XSD2JAVA)

        // add xsd2java task with group and a description
        project.task(XSD2JAVA,
                type: Xsd2JavaTask,
                group: 'Xsd2Java',
                description: 'Generate java source code from XSD files.') {
            classpath = xsd2javaConfiguration
        }

		// add cleanXsd task with group and a description
		project.task(CLEAN,
			type: CleanTask,
			group: 'Xsd2Java',
			description: 'Delete java source code generated from XSD files.')

        project.afterEvaluate {
			def jaxbVersion = ext.jaxbVersion
            project.dependencies {
                xsd2java "org.jvnet.jaxb2_commons:jaxb2-basics-ant:$jaxbVersion"
				xsd2java "org.slf4j:slf4j-api:+"
				xsd2java "org.slf4j:slf4j-log4j12:+"
				xsd2java "log4j:log4j:+"
            }

            // hook xsd2java into build cycle only if used
            if(project.xsd2java.xsdsToGenerate != null && project.xsd2java.xsdsToGenerate.size() > 0 ){
                project.sourceSets.main.java.srcDirs += project.xsd2java.generatedXsdDir
                project.compileJava.dependsOn project.xsd2java
            }

			if (ext.deleteGeneratedSourcesOnClean) {
				project.clean.dependsOn project.deleteGeneratedSources
			}
        }
    }
}
