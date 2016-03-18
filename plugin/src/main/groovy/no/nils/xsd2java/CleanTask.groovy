package no.nils.xsd2java

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CleanTask extends DefaultTask {

      @TaskAction
      public void clean(){
          project.xsd2java.generatedXsdDir.deleteDir()
      }
}