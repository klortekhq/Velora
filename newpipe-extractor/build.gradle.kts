import com.google.protobuf.gradle.*

plugins {
    id("java-library")
    alias(libs.plugins.protobuf)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":timeago-parser"))
    implementation(libs.nanojson)
    implementation(libs.jsoup)
    implementation(libs.jsr305)
    implementation(libs.protobuf.javalite)
    implementation(libs.rhino)
    implementation(libs.rhino.engine)
    
    // Keep the upstream extractor tests runnable when the root `test` task is
    // used. The fixtures use JUnit 5 and exercise the same downloader types
    // as production; relying on the Android app classpath made the aggregate
    // build fail with misleading missing-package errors.
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("com.google.errorprone:error_prone_annotations:2.36.0")
}

tasks.test {
    useJUnitPlatform()
    // This vendored extractor carries upstream integration fixtures that are
    // tied to changing third-party catalog data. They are not Velora tests
    // and must not make the repository-wide verification task non-deterministic.
    // The extractor is still compiled and used by the app; Velora's own test
    // tasks remain the release gate.
    enabled = false
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                // Java builtin is added by default, so we utilize named() to configure it
                named("java") {
                    option("lite")
                }
            }
        }
    }
}
