plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}

group = "kim.nzxy"
version = "1.4.3"

kotlin {
    jvmToolchain(25)
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
        intellijIdeaUltimate("2026.2")
        bundledPlugins(
            "com.intellij.java",
            "org.jetbrains.kotlin",
            "com.intellij.modules.json",
            "com.intellij.javaee.el"
        )
        bundledModules(
            "intellij.spring",
            "intellij.spring.el",
            "org.intellij.intelliLang"
        )
        pluginVerifier(version="1.384")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = "263.*"
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
