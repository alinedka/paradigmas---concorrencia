plugins {
    kotlin("jvm") version "2.2.20"
    application
}

group = "br.paradigmas"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    //biblioteca oficial de corrotinas da JetBrains
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

//rode um exemplo específico com: ./gradlew run -Pexemplo=parte1.threads.ThreadsCriacaoKt
//sem -Pexemplo roda todos (concorrencia.MainKt)
application {
    mainClass.set(
        "concorrencia." + (project.findProperty("exemplo") as String? ?: "MainKt")
    )
}
