plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

group = "kim.nzxy"
version = "1.3.0"

kotlin {
    jvmToolchain(17)
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    compileJava {
        options.encoding = "UTF-8"
    }
    compileTestJava {
        options.encoding = "UTF-8"
    }
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        releases()
        marketplace()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2022.3")
        instrumentationTools()
        bundledPlugins(
            "com.intellij.javaee.el",
            "com.intellij.spring.mvc",
            "org.intellij.intelliLang",
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "JavaScript"
        )
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223"
            untilBuild = "251.*"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
