import java.io.FileInputStream
import java.util.*

version = "1.16.11-EXPERIMENTAL-SNAPSHOT"
val javaVer = org.gradle.internal.jvm.Jvm.current().javaVersion?.majorVersion


buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("java-library")
}

apply {
    //    plugin("java")
//    plugin("idea")
//    plugin("maven-publish")
}

repositories {
    mavenCentral()
}

val testBootclasspath = configurations.create("testBootclasspath")
val rtLib6 = configurations.create("rtLib6")
val rtLib8 = configurations.create("rtLib8")
val moduleBuild = configurations.create("moduleBuild")


val props = Properties()
props.load(FileInputStream(File(project.rootDir, "testenvironment.properties")))

props.forEach { prop ->
    project.ext[prop.key.toString()] = prop.value
}

dependencies {
    //    compileOnly(gradleApi())

    this.add("moduleBuild", fileTree("lib/moduleBuild"))
    this.add("testBootclasspath", files(project.ext.get("test.location.bootclasspath")))
//    this.add("rtLib6", files("lib/openJDK6Environment/rt-openjdk6.jar"))
    val dependency = this.add("rtLib8", files("lib/openJDK8Environment/openjdk8_rt.jar"))
    compileOnly(dependency!!)

    compileOnly(fileTree("lib/build"))

    compileOnly(fileTree("lib/unexpected"))
    compileOnly(fileTree(project.ext.get("test.location.ecj") as String))
    //for debugging EclipsePatcher
//    compileOnly("org.ow2.asm:asm:5.0.1")
//    compileOnly("org.ow2.asm:asm-tree:5.0.1")
//    compileOnly("org.ow2.asm:asm-commons:5.0.1")

    testCompile("junit:junit:4.12")

    testCompile(fileTree("lib/test").include("*.jar").exclude("junit-junit.jar"))
    testCompile(fileTree(project.ext.get("test.location.ecj") as String))
    testCompile(fileTree(project.ext.get("test.location.javac") as String))

}


sourceSets {
    val mainOutput = sourceSets["main"].output
    val mainOutDir = mainOutput.classesDirs.singleFile
    val stubsStubs = create("stubsStubs") {
        java.srcDir(listOf("src/stubsstubs"))
    }
    val stubs = create("stubs") {
        java {
            srcDir(listOf("src/stubs", "src/javac-only-stubs"))
            compileClasspath = stubsStubs.output
        }
    }

    val utils1 = create("utils1") {
        java {
            srcDir(listOf("src/utils/"))
            exclude("lombok/javac/**")
//            outputDir = mainOutDir
        }
        compileClasspath = configurations["compileOnly"]
    }

    val utils2 = create("utils2") {
        java {
            srcDir(listOf("src/utils/"))
            include("lombok/javac/**")
//            outputDir = mainOutDir
        }
        compileClasspath = stubs.output + utils1.output + configurations["compileOnly"]
    }

    main {
        //        compileClasspath += /*lombok1.output + lombok2.output + */ + configurations["compileOnly"]
        java {
            compileClasspath = stubs.output + utils1.output + utils2.output + configurations["compileOnly"]
            srcDirs(listOf(
                    "src/launch",
                    "src/core",
                    "src/installer",
                    "src/eclipseAgent",
                    "src/delombok"
            ))
            exclude("**/*Transplants.java")
        }

        resources {
            srcDirs(sourceSets["main"].java.srcDirs)
            exclude("**/*.java")
        }
    }


    val class50 = create("class50") {
        compileClasspath += stubs.output + utils1.output + utils2.output + rtLib8 + configurations["compileOnly"] + mainOutput
        java {
            srcDirs(listOf(
                    "src/eclipseAgent"
            ))
            include("**/*Transplants.java", "lombok/launch/PatchFixesHider.java")
            outputDir = File(mainOutDir, "Class50")
        }
    }

    val core9 = create("core9") {
        java {
            srcDirs(listOf("src/core9"))
        }
    }

    val lombokMapstruct = create("lombokMapstruct") {
        java {
            srcDirs(listOf("src/j9stubs"))
            outputDir = File(mainOutDir, "lombok/secondaryLoading.SCL.lombok")
        }
        compileClasspath = sourceSets["main"].output
    }


    test {
        java {
            srcDir(listOf(
                    "test/bytecode/src",
                    "test/configuration/src",
                    "test/core/src",
                    "test/transform/src"
            ))
        }
        compileClasspath += sourceSets["main"].compileClasspath + utils1.output + utils2.output
        resources {
            srcDir(listOf(
                    "test/bytecode/resource",
                    "test/configuration/resource",
                    "test/transform/resource"
            ))
        }
    }
    all {
        //exclude empty dirs from source sets
        tasks[processResourcesTaskName].setProperty("includeEmptyDirs", false)
    }
}


val stubsOutput = sourceSets["stubs"].output.classesDirs
tasks.withType<JavaCompile> {
    val stubsStubsOutput = sourceSets["stubsStubs"].output.classesDirs

    options.encoding = "UTF-8"
//    options.compilerArgs = listOf("--release", "6")
    sourceCompatibility = "1.6"
    targetCompatibility = sourceCompatibility

    if (name in listOf("compileStubsStubsJava", "compileStubsJava")) {
        options.compilerArgs = listOf("--release", "8")
        sourceCompatibility = "1.5"
        targetCompatibility = sourceCompatibility
        options.bootstrapClasspath = rtLib8.asFileTree
    }

    if (this.name == "compileUtils1Java") {
        options.compilerArgs = listOf("--release", "6")
        sourceCompatibility = "1.5"
        targetCompatibility = sourceCompatibility
        options.bootstrapClasspath = stubsStubsOutput + stubsOutput + rtLib8.asFileTree
    }

    if (this.name == "compileUtils2Java") {
        options.compilerArgs = listOf("--release", "6")
        sourceCompatibility = "1.6"
        targetCompatibility = sourceCompatibility
        options.bootstrapClasspath = stubsStubsOutput + stubsOutput + rtLib8.asFileTree
    }

    if (name == "compileLombok1Java") {
        doFirst {
            options.compilerArgs = listOf("--release", "8")
        }
        sourceCompatibility = "1.5"
        targetCompatibility = sourceCompatibility
        options.bootstrapClasspath = stubsStubsOutput + stubsOutput + rtLib8.asFileTree
    }

    if (name == "compileCore9Java") {
        dependsOn("compileJava")
        mustRunAfter("coreBySpiProcessor")

        val mainOutput = sourceSets["main"].output.classesDirs.asPath

        doFirst {
            options.compilerArgs = listOf(
                    "--release", "9",
                    "-Xlint:none",
                    "-d", mainOutput,
                    "--module-path", moduleBuild.asPath
            )
        }

        val source = this.source
        val destinationDir = this.destinationDir

        doLast {
            source.forEach { f ->
                val fileName = f.nameWithoutExtension + ".class"
                val classFile = File(mainOutput, fileName)
                val copyTo = classFile.copyTo(File(destinationDir, fileName))
                System.out.println("$classFile->$copyTo")
                classFile.delete()

            }
        }
    }

    if (name == "compileLombokMapstructJava") {
        doLast {
            this.outputs.files.asFileTree.filter({ f -> f.isFile }).forEach { f ->
                f.renameTo(File(f.parentFile, f!!.nameWithoutExtension + ".SCL.lombok"))
            }
        }
    }


    if (name == "compileJava") {
        options.bootstrapClasspath =  stubsOutput + rtLib8.asFileTree
    }
}

tasks.create<JavaCompile>("coreBySpiProcessor") {
    dependsOn("compileJava")
//    options.isVerbose = true
    options.compilerArgs = listOf(
            "--release", "9",
            "-proc:only",
            "-processor", "org.mangosdk.spi.processor.SpiProcessor"
    )
    val main = sourceSets["main"]
    classpath = main.compileClasspath
    options.annotationProcessorPath = configurations["compileOnly"]
    source = main.allSource
    destinationDir = main.output.classesDirs.singleFile
}

tasks.withType<Test> {
    systemProperty("file.encoding", "utf-8")
}

val echoProcessor = tasks.create("echoProcessor") {
    doLast {
        val path = sourceSets["main"].output.classesDirs.singleFile
        val file = File(path, "/META-INF/services/javax.annotation.processing.Processor")
        file.parentFile.mkdirs()
        file.delete()
//        file.createNewFile()
        file.appendText("lombok.launch.AnnotationProcessorHider\$AnnotationProcessor" +
                "\n" +
                "lombok.launch.AnnotationProcessorHider\$ClaimingProcessor"
        )
    }
}

tasks["classes"].dependsOn(echoProcessor)

tasks.withType<Jar> {
    if (name == "jar") {
        val utilsOutput = sourceSets["utils1"].output + sourceSets["utils2"].output + sourceSets["core9"].output
        from(utilsOutput)
        dependsOn("compileCore9Java", "compileClass50Java", "compileLombokMapstructJava", "coreBySpiProcessor")
        destinationDir = file("dist")
        archiveName = "$baseName.$extension"
        manifest {
            attributes(
                    mapOf("Premain-Class" to "lombok.launch.Agent",
                            "Agent-Class" to "lombok.launch.Agent",
                            "Can-Redefine-Classes" to "true",
                            "Main-Class" to "lombok.launch.Main",
                            "Lombok-Version" to archiveVersion.get())
            )
        }
    }
}

tasks.withType<Test> {
    val jar = tasks["jar"] as Jar

    //        "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
    this.jvmArgs(listOf(
            "-javaagent:${jar.archivePath}",
//            "-Ddelombok.bootclasspath", testBootclasspath.asPath,
            "--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-opens", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"
    ))
    //use all classes except RunAllTests
//    include("lombok/RunAllTests.class")
    include("lombok/transform/RunTransformTests.class")
    testLogging.showStandardStreams = true
}

//for supporing eclipse compiler
tasks["test"].dependsOn(tasks["jar"])

