package no.nils.xsd2java

import org.gradle.api.tasks.Input;

class Xsd2JavaPluginExtension {
	String jaxbVersion = "+"
	boolean deleteGeneratedSourcesOnClean = false
}
