plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "kim.nzxy"
version = "1.1.0"

tasks {
    buildSearchableOptions{
        enabled = false
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    compileTestJava{
        options.encoding = "UTF-8"
    }
}

repositories {
    mavenCentral()
}

intellij {
    version.set("2021.3")
    type.set("IU") // Target IDE Platform

    plugins.set(
        listOf(
            "com.intellij.javaee.el",
            "com.intellij.spring.mvc",
            "org.intellij.intelliLang",
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "JavaScript"
        )
    )
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("252.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
