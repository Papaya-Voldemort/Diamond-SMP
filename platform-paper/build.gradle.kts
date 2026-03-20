dependencies {
    api(project(":core-api"))
    implementation(project(":core-domain"))
    implementation(project(":core-service"))
    implementation(project(":infra-command"))
    implementation(project(":infra-events"))
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}
