import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    jacoco
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.adarshr.test-logger") version "2.1.1"
    id("io.franzbecker.gradle-lombok") version "4.0.0"
    `java-library`
    `maven-publish`
    signing
}

group = "de.atextor"
description = "Library for pretty printing RDF/Turtle documents"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.jena:jena-core:3.17.0")
    implementation("io.vavr:vavr:0.10.3")
    implementation("org.slf4j:slf4j-api:1.7.30")

    annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("net.jqwik:jqwik:1.4.0")

    testAnnotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.3.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

java {
    withJavadocJar()
    withSourcesJar()
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(arrayOf("--release", "11"))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    sourceCompatibility = "15"
    targetCompatibility = "11"
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
    sourceCompatibility = "15"
    targetCompatibility = "11"
}

tasks.test {
    useJUnitPlatform()
    ignoreFailures = false
    failFast = true
    maxHeapSize = "1G"
}

jacoco {
    toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        xml.destination = file("${buildDir}/reports/jacoco/report.xml")
        html.isEnabled = true
        html.destination = file("${buildDir}/reports/jacoco/html-report")
    }
}

tasks.javadoc {
    (options as CoreJavadocOptions).addStringOption("Xdoclint:accessibility,html,syntax,reference", "-quiet")
    options.encoding = "UTF-8"
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
    shouldRunAfter(tasks.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "turtle-formatter"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("turtle-formatter")
                description.set(project.description)
                url.set("https://github.com/atextor/turtle-formatter")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("atextor")
                        name.set("Andreas Textor")
                        email.set("mail@atextor.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/atextor/turtle-formatter.git")
                    developerConnection.set("scm:git:ssh://github.com:atextor/turtle-formatter.git")
                    url.set("https://github.com/atextor/turtle-formatter/tree/main")
                }
                issueManagement {
                    url.set("https://github.com/atextor/turtle-formatter/issues")
                    system.set("GitHub")
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}

// This configuration will read the signing PGP key and password from environment variables
// ORG_GRADLE_PROJECT_signingKey (in ASCII-armored format) and ORG_GRADLE_PROJECT_signingPassword
signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

