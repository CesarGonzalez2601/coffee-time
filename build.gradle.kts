plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "com.coffeetime"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.coffeetime.MainKt"
}

tasks.named<JavaExec>("run") {
    // La app lee de stdin (login y menus); sin esto readln() recibe EOF.
    standardInput = System.`in`

    // Los acentos del menu se rompen en consolas que no son UTF-8.
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )
}

tasks.test {
    useJUnitPlatform()
}