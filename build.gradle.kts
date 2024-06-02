plugins {
    java
    `maven-publish`
    alias(libs.plugins.lavalink)
}

group = "id.my.nezu"
version = "0.1.0"

lavalinkPlugin {
    name = "additional-music-source"
    apiVersion = libs.versions.lavalink.api
    serverVersion = libs.versions.lavalink.server
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}

dependencies {
    // add your dependencies here
    implementation("org.json:json:20220320")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
