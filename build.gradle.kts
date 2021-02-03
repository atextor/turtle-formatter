import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    jacoco
    id("com.github.ben-manes.versions") version "0.33.0"
    id("com.adarshr.test-logger") version "2.1.0"
    id("io.franzbecker.gradle-lombok") version "4.0.0"
}

repositories {
    jcenter()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("org.apache.jena:jena:3.16.0")
    implementation("org.apache.jena:jena-core:3.16.0")
    implementation("io.vavr:vavr:0.10.3")
    implementation("org.slf4j:slf4j-api:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.assertj:assertj-core:3.17.2")
    testImplementation("net.jqwik:jqwik:1.3.6")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
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

tasks {
    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "15"
    }

    compileTestJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "15"
    }

    test {
        useJUnitPlatform()
        ignoreFailures = false
        failFast = true
    }
}
