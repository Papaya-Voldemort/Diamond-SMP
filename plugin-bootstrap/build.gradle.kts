plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":platform-paper"))
    implementation(project(":core-api"))
    implementation(project(":core-domain"))
    implementation(project(":core-service"))
    implementation(project(":infra-config"))
    implementation(project(":infra-persistence"))
    implementation(project(":infra-command"))
    implementation(project(":infra-events"))

    compileOnly(libs.paper.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("Diamond-SMP")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
