plugins {
    id("java")
}

group = "irai.mod.dynamicfloatingdamageformatter"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Provide the Hytale server jar locally in libs/ to compile.
    compileOnly(files("libs/HytaleServer.jar"))
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("manifest.json")
    // Core API jar (no adapter system)
    exclude("irai/mod/reforge/Entity/Events/**")
}

tasks.register<Jar>("jarWithAdapter") {
    group = "build"
    description = "Builds a plug-and-play jar with the damage adapter included."
    archiveClassifier.set("with-adapter")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    from("manifest.json")
}
