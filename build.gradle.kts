import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    jacoco
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.adarshr.test-logger") version "2.1.1"
    id("io.franzbecker.gradle-lombok") version "4.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.jena:apache-jena-libs:3.17.0")
    implementation("io.vavr:vavr:0.10.3")
    implementation("org.slf4j:slf4j-api:1.7.30")

    annotationProcessor("com.github.bsideup.jabel:jabel-javac-plugin:0.3.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("net.jqwik:jqwik:1.4.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

jacoco {
    toolVersion = "0.8.6"
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

tasks {
    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "15"
        targetCompatibility = "11"
    }

    compileTestJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "15"
        targetCompatibility = "11"
    }

    test {
        useJUnitPlatform()
        ignoreFailures = false
        failFast = true
    }

    javadoc {
        (options as CoreJavadocOptions).addStringOption("Xdoclint:accessibility,html,syntax,reference", "-quiet")
        options.encoding = "UTF-8"
        shouldRunAfter(test)
    }

    jacocoTestReport {
        reports {
            xml.isEnabled = true
            xml.destination = file("${buildDir}/reports/jacoco/report.xml")
            html.isEnabled = true
            html.destination = file("${buildDir}/reports/jacoco/html-report")
        }
    }
}
