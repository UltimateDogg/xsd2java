package no.nils.xsd2java

import java.io.File
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

import groovy.io.FileType;

/**
 *
 */
class Xsd2JavaTask extends DefaultTask {
	private static final NEWLINE = System.getProperty("line.separator");
    // user properties
	@Input
	String encoding = Charset.defaultCharset().name()
	@Input
	boolean stabilize = false
	@Input
	boolean stabilizeAndMergeObjectFactory = false

    @InputDirectory
    File xsdDir = new File("src/main/resources")

    @OutputDirectory
    File generatedXsdDir = new File(Xsd2JavaPlugin.DEFAULT_DESTINATION_DIR)

    def xsdsToGenerate
    
    /**
     * The Locale for the generated Java classes.
     */
    Locale locale = Locale.getDefault()
    
    // build internal properties
    Configuration classpath

	public Xsd2JavaTask() {
		project.afterEvaluate {
			inputs.property("xsds", xsdsToGenerate.toString())
		}
	}
	
    @TaskAction
    def xsd2java() {
		deleteOutputFolders()
		MessageDigest md5 = MessageDigest.getInstance("MD5")

		File tmpDir = new File(project.getBuildDir(), "xsd2java")
		tmpDir.deleteDir()
		
        if(classpath == null) {
            classpath = project.configurations.getByName(Xsd2JavaPlugin.XSD2JAVA)
        }
		
		ant.taskdef(name: 'xjc', classname: 'org.jvnet.jaxb2_commons.xjc.XJC2Task', classpath: classpath.asPath)

        if(!generatedXsdDir.exists()){
            generatedXsdDir.mkdirs()
        }

        xsdsToGenerate.each { args ->
			String xsdPath = md5.digest(args[-1].toString().bytes).encodeHex().toString()
			File targetDir = new File(tmpDir, xsdPath)
			targetDir.mkdirs()
			
			def options = [
				schema: args[-1],
				encoding: encoding
			]
			
			args.add(args.size - 1, '-d')
			args.add(args.size - 1, targetDir)
			
            String[] xsd2JavaArgs = new String[args.size()];
            for (int i = 0; i < args.size(); i++) {
                xsd2JavaArgs[i] = args[i]
            }
            runWithLocale(this.locale) { ->
	        	try {
					println captureAntOutput(ant) {
						xjc(options)  {
							xsd2JavaArgs.each { val ->
								arg(value: val)
							}
						}
	        		}
	        	} catch (Exception e) {
		            throw new TaskExecutionException(this, e)
		        }
		    }

        	copyToOutputDir(targetDir)
    	}
    }

    protected void runWithLocale(Locale locale, Closure<Void> closure) {
        // save the current default locale – will be set back at the end
        Locale currentDefaultLocale = Locale.getDefault()
        try {
            // set the wanted locale for the generated java classes
            Locale.setDefault(locale)

            closure()
        }
        finally {
            // set the default locale back to the previous default
            Locale.setDefault(currentDefaultLocale)
        }
    }

	protected void deleteOutputFolders() {
		Set<String> packagePaths = findPackagePaths();
		if (packagePaths.isEmpty()) {
			packagePaths.add(""); // add root if no package paths
		}

		Set<File> packageTargetDirs = packagePaths.collect { subPath -> new File(generatedXsdDir, subPath) }
		getLogger().info("Clear target folders {}", packageTargetDirs);
		getProject().delete(packageTargetDirs);
	}

	private Set<String> findPackagePaths() {
		Set<String> packagePaths = new HashSet<>();
		for (List<String> args : xsdsToGenerate) {
			int packageArgIdx = args.indexOf("-p");
			int packageIx = packageArgIdx+1;
			if (packageArgIdx != -1 && args.size() >= packageIx) {
				String pathPath = args.get(packageIx).replace(".", "/");
				packagePaths.add(pathPath);
			}
		}
		return packagePaths;
	}

	protected void copyToOutputDir(File srcDir) {
		int srcPathLength = srcDir.getAbsolutePath().size() + 1
		
		srcDir.eachFileRecurse(FileType.FILES) { file ->
			String relPath = file.getAbsolutePath().substring(srcPathLength)
			File target = new File(generatedXsdDir, relPath)
			
			switchToEncoding(file)
			
			if (stabilizeAndMergeObjectFactory) {
				mergeAndStabilizeObjectFactory(file, target)
			} else {
				project.ant.copy(file: file, tofile: target)
			}			
		}
	}

	protected void switchToEncoding(File file) {
		List<String> lines = file.getText().split(NEWLINE)
		file.delete()

		if (stabilize) {
			stripCommentDates(lines)
			stabilizeCommentLinks(file, lines)
			stabilizeXmlElementRef(file, lines)
			stabilizeXmlSeeAlso(file, lines)
		}

		String text = lines.join(NEWLINE) + NEWLINE  // want empty line last
		file.withWriter(encoding) { w -> w.write(text) }
	}

	void stripCommentDates(List<String> lines) {
		String prevLine = "";
		for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
			String l = lix.next()
			if (prevLine.contains("This class was generated") && l.startsWith(" * 201")) {
				lix.remove();
			}
			prevLine = l;
		}
	}
	
	void stabilizeCommentLinks(File file, List<String> lines) {
		for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
			String l = lix.next()
			
			if (l.contains("* {@link")) {
				int start = lix.previousIndex()
				
				while (lix.hasNext()) {
					l = lix.next()
					if (!l.contains("* {@link")) {
						int end = lix.previousIndex()
						
						List<String> subList = lines.subList(start, end);
						Collections.sort(subList)
						
						break
					}
				}
			}
		}
	}
	
	void stabilizeXmlSeeAlso(File file, List<String> lines) {
		String seeAlsoStart = "@XmlSeeAlso({"
		String seeAlsoEnd = "})"
		for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
			String l = lix.next()
			
			if (l.startsWith(seeAlsoStart) && l.endsWith(seeAlsoEnd)) {
				List<String> classes = l.replace(seeAlsoStart, "").replace(seeAlsoEnd, "").split(",").collect { it.trim() }
				String sortedClasses = seeAlsoStart + classes.sort().join(", ") + seeAlsoEnd
				lix.set(sortedClasses)
			}
		}
	}
	
	void stabilizeXmlElementRef(File file, List<String> lines) {
		String prevLine = "";
		for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
			String l = lix.next()
			
			if (l.contains("@XmlElementRef") && prevLine.contains("@XmlElementRefs")) {
				int start = lix.previousIndex()
	
				while (lix.hasNext()) {
					l = lix.next()
					if (!l.contains("@XmlElementRef")) {
						int end = lix.previousIndex()
						
						List<String> subList = lines.subList(start, end);
						Collections.sort(subList)
						
						// Fix ,-separation of lines
						for (ListIterator<String> subLix = subList.listIterator(); subLix.hasNext();) {
							String line = subLix.next()
							
							line = line.replaceFirst(',$', "")
							if (subLix.hasNext()) {
								line = line + ","
							}
							subLix.set(line)
						}
						break
					}
				}
			}
			prevLine = l;
		}
	}	
	
	protected void mergeAndStabilizeObjectFactory(File src, File target) {
		if (!target.exists()) {
			target.getParentFile().mkdirs()
			project.ant.copy(file: src, tofile: target)
			stabilizeObjFacWithItself(target);
		} else {
			stabilizeObjFacWithTarget(src, target)
		}
	}

	private void stabilizeObjFacWithItself(File target) {
		if (isObjectFactory(target)) {
			getLogger().info(" stabilize ${target}")
			ObjectFactoryMerger.merge(target, target, encoding)
		}
	}

	private stabilizeObjFacWithTarget(File src, File target) {
		if (isObjectFactory(src) && src.getText(encoding) != target.getText(encoding)) {
			getLogger().info(" merge     ${target}")
			ObjectFactoryMerger.merge(src, target, encoding)
		}
	}

	private boolean isObjectFactory(File f) {
		return "ObjectFactory.java".equals(f.getName());
	}
	
	def captureAntOutput(ant, Closure command) {
		def buffer = new ByteArrayOutputStream()
		def captureStream = new PrintStream(buffer, true, "UTF-8")
		def listener = new org.apache.tools.ant.DefaultLogger(
				errorPrintStream: captureStream,
				outputPrintStream: captureStream,
				messageOutputLevel: org.apache.tools.ant.Project.MSG_INFO
		)
	
		ant.project.addBuildListener(listener)
		project.configure(ant, command)
		ant.project.removeBuildListener(listener)
	
		return buffer.toString("UTF-8");
	}
}
