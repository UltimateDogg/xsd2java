
# Work in progress, not uploaded to bintray/jcenter yet

xsd2java gradle plugin
=========

Gradle plugin for generating java from xsd schemas

### Issues
If you have any issues with the plugin, please file an issue at github, https://github.com/nilsmagnus/wsdl2java/issues

### Contribution
Contributions are welcome as long as they are sane.

#### Contributors
- Nils Larsg√•rd , https://github.com/nilsmagnus
- Mats Faugli, https://github.com/fowlie
- Thorben Schiller, https://github.com/thorbolo
- Stefan Kloe, https://github.com/Pentadrago
- Mattias Rundgren, https://github.com/matrun
- "s-doering", https://github.com/s-doering

### CXF
This plugin uses the xjc tool to do the actual work.

### Tasks

| Name | Description | Dependecy |
| ---- | ----------- | --------- |
| xsd2java | Generate java source from wsdl-files | CompileJava depends on xsd2java |
| deleteGeneratedXsdSources | Delete all generated sources | Clean depends on deleteGeneratedXsdSources |

## Usage

To use this plugin, you must
- modify your buildscript to have dependencies to the plugin
- apply the plugin
- set the properties of the plugin
- add the generated sources to your sourceset

### Applying the plugin

    buildscript{
        repositories{
            jcenter()
            mavenCentral()
        }
        dependencies {
            classpath 'no.nils:xsd2java:0.6'
        }
    }
    apply plugin: 'no.nils.xsd2java'

### Plugin options

| Option | Default value | Description |
| ------ | ------------- | ----------- |
| jaxbVersion | "+" | Controls the JAXB version used to generate code.
| deleteGeneratedSourcesOnClean | true | If you want to keep the generated sources under version control, set this option to false. |


Example of specifying another JAXB version:

    xsd2javaExt {
        jaxbVersion = "0.11.0"
    }


Example of retaining the generated sources on clean:

    xsd2javaExt {
        deleteGeneratedSourcesOnClean = false
    }
    
### Options for xsd2java
| Option | Default value | Description |
| ------ | ------------- | ----------- |
| generatedXsdDir | "generatedsources/src/main/java" | Destination directory for generated sources sources to be placed. |
| xsdDir | src/main/resources | Define the xsd files directory to support incremental build. This means that the task will be up-to-date if nothing in this directory has changed. |
| wsdlsToGenerate | empty | This is the main input to the plugin that defines the xsds to process. It is a list of arguments where each argument is a list of arguments to process a xsd-file. The xsd-file with full path is the last argument. The array can be supplied with the same options as described for the jaxb plugin(https://jaxb.java.net/2.2.4/docs/xjc.html). |
| encoding | platform default encoding | Set the encoding name for generated sources, such as EUC-JP or UTF-8. |
| locale | Locale.getDefault() | The locale for the generated sources ñ especially the JavaDoc. This might be necessary to prevent differing sources due to several development environments. |
| stabilizeAndMergeObjectFactory| false | If multiple XSDs target the same package, merge their ObjectFactory.java classes |

Example setting of options:

    xsd2java {
        generatedXsdDir = file("generatedsources/xsd2java")  // target directory for generated source coude
        xsdDir = file("src/main/resources/myXsdFiles") // define to support incremental build
        xsdsToGenerate = [   //  2d-array of xsds and xjc-parameters
                    ['src/main/resources/xsd/firstxsd.xsd'],
                    ['-p','no.nils.xsd2java.sample','-verbose','src/main/resources/xsd/secondxsd.xsd']
            ]
        locale = Locale.GERMANY
        encoding = 'utf-8'
    }



## Complete example usage
This is a an example of a working build.gradle for a java project. You can also take a look at this projects submodule "consumer" which has a working xsd compiling.

    buildscript{
        repositories{
            jcenter()
            mavenCentral()
        }
        dependencies {
            classpath 'no.nils:xsd2java:0.6'
        }
    }

    apply plugin :'java'
    apply plugin :'no.nils.xsd2java'

    repositories{
        mavenCentral()
    }

    dependencies(){
        testCompile 'junit:junit:+'
    }

    xsd2java {
        generatedXsdDir = file("generatedsources/xsd2java")  // target directory for generated source coude
        xsdDir = file("src/main/resources/myXsdFiles") // define to support incremental build
        xsdsToGenerate = [   //  2d-array of xsds and xjc-parameters
                    ['src/main/resources/xsd/firstxsd.xsd'],
                    ['-p','no.nils.xsd2java.sample','-verbose','src/main/resources/xsd/secondxsd.xsd']
            ]
        locale = Locale.GERMANY
        encoding = 'utf-8'
    }

### A notice on multi-module projects

Instead of referring to absolute paths in your build-file, try using $projectDir as a prefix to your files and directories. As shown in the "Complete example usage".
