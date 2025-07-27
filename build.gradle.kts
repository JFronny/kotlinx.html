@file:OptIn(ExperimentalWasmDsl::class)

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.io.ByteArrayOutputStream

/**
 * This build script supports the following parameters:
 * -PversionTag - works together with "branch-build" profile and overrides "-SNAPSHOT" suffix of the version.
 */
plugins {
    kotlin("multiplatform") version "2.0.21"
    id("maven-publish")
    id("signing")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("de.undercouch.download") version "5.6.0"
}

group = "org.jetbrains.kotlinx"
version = "0.12.0-jf.1"

/**
 * If "release" profile is used the "-SNAPSHOT" suffix of the version is removed.
 */
if (hasProperty("release")) {
    val versionString = version as String
    if (versionString.endsWith("-SNAPSHOT")) {
        version = versionString.replace("-SNAPSHOT", "")
    }
}

/**
 * Handler of "versionTag" property.
 * Required to support Maven and NPM repositories that doesn't support "-SNAPSHOT" versions. To build and publish
 * artifacts with specific version run "./gradlew -PversionTag=my-tag" and the final version will be "0.6.13-my-tag".
 */
if (hasProperty("versionTag")) {
    val versionString = version as String
    val versionTag = properties["versionTag"]
    if (versionString.endsWith("-SNAPSHOT")) {
        version = versionString.replace("-SNAPSHOT", "-$versionTag")
        logger.lifecycle("Project will be built with version '$version'.")
    } else {
        error("Could not apply 'versionTag' together with non-snapshot version.")
    }
}

if (hasProperty("releaseVersion")) {
    version = properties["releaseVersion"] as String
}

val publishingUser = System.getenv("PUBLISHING_USER")
val publishingPassword = System.getenv("PUBLISHING_PASSWORD")
val publishingUrl = System.getenv("PUBLISHING_URL")

publishing {
    publications {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/JFronny/kotlinx.html")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
            if (publishingUser == null) return@repositories
            maven {
                url = uri(publishingUrl)
                credentials {
                    username = publishingUser
                    password = publishingPassword
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

val emptyJar = tasks.register<org.gradle.jvm.tasks.Jar>("emptyJar") {
    archiveAppendix.set("empty")
}

kotlin {
    jvm {
        mavenPublication {
            groupId = group as String
            pom { name = "${project.name}-jvm" }

            artifact(emptyJar) {
                classifier = "javadoc"
            }
        }
    }
    js {
        moduleName = project.name
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        mavenPublication {
            groupId = group as String
            pom { name = "${project.name}-js" }
        }
    }
    wasmJs {
        moduleName = project.name
        browser()

        mavenPublication {
            groupId = group as String
            pom { name = "${project.name}-wasm-js" }
        }
    }
    wasmWasi {
        nodejs()

        mavenPublication {
            groupId = group as String
            pom { name = "${project.name}-wasm-wasi" }
        }
    }

    mingwX64()
    linuxX64()
    linuxArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    macosX64()
    macosArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jsCommon") {
                withJs()
                withWasmJs()
            }
        }
    }

    metadata {
        mavenPublication {
            groupId = group as String
            artifactId = "${project.name}-common"
            pom {
                name = "${project.name}-common"
            }
        }
    }
}

kotlin {
    jvmToolchain(8)

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes += sortedMapOf(
            "Built-By" to System.getProperty("user.name"),
            "Build-Jdk" to System.getProperty("java.version"),
            "Implementation-Vendor" to "JetBrains s.r.o.",
            "Implementation-Version" to archiveVersion.get(),
            "Created-By" to GradleVersion.current()
        )
    }
}

tasks.register<Task>("generate") {
    group = "source-generation"
    description = "Generate tag-handling code using tags description."

    doLast {
        kotlinx.html.generate.generate(
            pkg = "kotlinx.html",
            todir = "src/commonMain/kotlin/generated",
            jsdir = "src/jsMain/kotlin/generated",
            wasmJsDir = "src/wasmJsMain/kotlin/generated"
        )
        kotlinx.html.generate.generateJsTagTests(
            jsdir = "src/jsTest/kotlin/generated",
            wasmJsDir = "src/wasmJsTest/kotlin/generated",
        )
    }
}

publishing {
    publications {
        configureEach {
            if (this is MavenPublication) {
                pom.config()
            }
        }
    }
}

typealias MavenPomFile = MavenPom

fun MavenPomFile.config(config: MavenPomFile.() -> Unit = {}) {
    config()

    url = "https://github.com/Kotlin/kotlinx.html"
    name = "kotlinx.html"
    description = "A kotlinx.html library provides DSL to build HTML to Writer/Appendable or DOM at JVM and browser (or other JavaScript engine) for better Kotlin programming for Web."

    licenses {
        license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
    }

    scm {
        connection = "scm:git:git@github.com:Kotlin/kotlinx.html.git"
        url = "https://github.com/Kotlin/kotlinx.html"
        tag = "HEAD"
    }

    developers {
        developer {
            name = "Sergey Mashkov"
            organization = "JetBrains s.r.o."
            roles to "developer"
        }

        developer {
            name = "Anton Dmitriev"
            organization = "JetBrains s.r.o."
            roles to "developer"
        }
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = true
}

val signingKey = System.getenv("SIGN_KEY_ID")
val signingKeyPassphrase = System.getenv("SIGN_KEY_PASSPHRASE")

if (!signingKey.isNullOrBlank()) {
    project.ext["signing.gnupg.keyName"] = signingKey
    project.ext["signing.gnupg.passphrase"] = signingKeyPassphrase

    signing {
        useGpgCmd()
        sign(publishing.publications)
    }
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().ignoreScripts = false
}

tasks.named("jsBrowserTest") {
    dependsOn("wasmJsTestTestDevelopmentExecutableCompileSync")
}

tasks.named("wasmJsBrowserTest") {
    dependsOn("jsTestTestDevelopmentExecutableCompileSync")
}

val buildSrcResources = projectDir.resolve("buildSrc/src/main/resources")
val downloadAttributes by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML attributes description from w3."
    src("https://www.w3.org/TR/html401/index/attributes.html")
    dest(buildSrcResources.resolve("attributes.html"))
    overwrite(true)
    doLast {
        logger.lifecycle("HTML attributes description downloaded successfully.")
    }
}

val downloadElements by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML elements description from w3."
    src("https://www.w3.org/TR/html401/index/elements.html")
    dest(buildSrcResources.resolve("elements.html"))
    overwrite(true)
    doLast {
        logger.lifecycle("HTML elements description downloaded successfully.")
    }
}

val downloadHtmlTagTableGen by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML tag table generator script."
    src("https://raw.githubusercontent.com/JetBrains/intellij-community/refs/heads/master/xml/impl/resources/com/intellij/xml/util/documentation/htmlTagTableGen.pl")
    dest(buildSrcResources.resolve("htmlTagTableGen.pl"))
    overwrite(true)
    doLast {
        logger.lifecycle("HTML tag table generator script downloaded successfully.")
    }
}

val htmlTagTableGen by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Generate HTML tag table from the tags description."
    dependsOn(downloadAttributes, downloadElements, downloadHtmlTagTableGen)
    workingDir = buildSrcResources
    commandLine("perl", "htmlTagTableGen.pl")
    inputs.files(downloadAttributes, downloadElements, downloadHtmlTagTableGen)
    outputs.file(buildSrcResources.resolve("htmltable.xml"))
}

val downloadHtml5 by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML5 tags description from w3."
    src("https://html.spec.whatwg.org/multipage/indices.html")
    dest(buildSrcResources.resolve("html5_new.html"))
    overwrite(true)
    doLast {
        logger.lifecycle("HTML5 tags description downloaded successfully.")
    }
}

val preprocessHtml5 by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Preprocess HTML5 tags description to remove unnecessary parts."
    dependsOn(downloadHtml5)
    workingDir = buildSrcResources
    commandLine("python", "preprocess.py")
    inputs.files(downloadHtml5)
    outputs.file(buildSrcResources.resolve("html5.html"))
}

val downloadHtml5TagTableGen by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML5 tag table generator script."
    src("https://raw.githubusercontent.com/JetBrains/intellij-community/refs/heads/master/xml/impl/resources/com/intellij/xml/util/documentation/html5TagTableGen.rb")
    dest(buildSrcResources.resolve("html5TagTableGen.rb"))
    overwrite(true)
    doLast {
        logger.lifecycle("HTML5 tag table generator script downloaded successfully.")
    }
}

val html5TagTableGen by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Generate HTML tag table from the tags description."
    dependsOn(htmlTagTableGen, preprocessHtml5, downloadHtml5TagTableGen)
    workingDir = buildSrcResources
    commandLine("ruby", "html5TagTableGen.rb")
    inputs.files(htmlTagTableGen, preprocessHtml5, downloadHtml5TagTableGen)
    outputs.file(buildSrcResources.resolve("html5table.xml"))
    val baos = ByteArrayOutputStream()
    standardOutput = baos
    doLast {
        buildSrcResources.resolve("html5table.xml").writeBytes(baos.toByteArray())
    }
}

val downloadEntitiesJson by tasks.registering(Download::class) {
    group = "build setup"
    description = "Download HTML entities description in JSON format."
    src("https://html.spec.whatwg.org/entities.json")
    dest(buildSrcResources.resolve("entities.json"))
    overwrite(true)
}

val extractEntities by tasks.registering(Exec::class) {
    group = "build setup"
    description = "Extract HTML entities from JSON file."
    dependsOn(downloadEntitiesJson)
    workingDir = buildSrcResources
    commandLine("python", "extract_entities.py")
    inputs.files(downloadEntitiesJson, buildSrcResources.resolve("extract_entities.py"))
    outputs.file(buildSrcResources.resolve("entities.txt"))
}

tasks.register("regenerateXml") {
    group = "build setup"
    description = "Regenerate XML files for the project."
    dependsOn(htmlTagTableGen, html5TagTableGen, extractEntities)
}
