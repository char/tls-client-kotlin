plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
}

group = "codes.som"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<JavaCompile> {
    options.release = 11
}

dependencies {
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
}

tasks.processResources {
    from("../go/dist") {
        include("*.dll", "*.so", "*.dylib")
        into("com/github/bogdanfinn/tlsclient/resources")
    }
}
