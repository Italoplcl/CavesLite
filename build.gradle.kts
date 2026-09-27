plugins {
    java
}

group = "dev.caveslite"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Purpur's API includes everything from Paper, Spigot and Bukkit, plus
    // Purpur's own extras (better mob AI options, more config, etc.).
    compileOnly("org.purpurmc.purpur:purpur-api:26.3.build.+")
    // Both are optional at runtime (see plugin.yml softdepend) - only used
    // if the server actually has Vault and/or PlaceholderAPI installed.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("DangerousCavesLite")
    from("LICENSE")
}
