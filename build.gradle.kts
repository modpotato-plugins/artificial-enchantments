plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "io.artificial"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    
    // PaperMC repository for Paper API
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    
    // FoliaLib repository
    maven {
        name = "tcoded-releases"
        url = uri("https://repo.tcoded.com/releases")
    }
    
    // Item-NBT-API repository (CodeMC)
    maven {
        name = "codemc"
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }
}

dependencies {
    // Paper API 1.21+ - compile only (provided by server)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    
    // FoliaLib - must be shaded and relocated
    implementation("com.tcoded:FoliaLib:0.5.1")
    
    // Item-NBT-API - implementation for compilation
    implementation("de.tr7zw:item-nbt-api:2.14.1")
    
    // Annotations
    compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.compileJava {
    options.release.set(21)
}

tasks.processResources {
    val projectProps = mapOf(
        "version" to project.version,
        "group" to project.group.toString()
    )
    inputs.properties(projectProps)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(projectProps)
    }
}

// Shadow JAR configuration disabled for now due to plugin issues
// tasks.shadowJar {
//     archiveClassifier.set("")
//     relocate("com.tcoded.folialib", "io.artificial.enchantments.lib.folialib")
// }
//
// tasks.build {
//     dependsOn(tasks.shadowJar)
// }
