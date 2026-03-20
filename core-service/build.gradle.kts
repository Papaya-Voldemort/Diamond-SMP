dependencies {
    api(project(":core-api"))
    implementation(project(":core-domain"))
    implementation(project(":infra-config"))
    compileOnly(libs.paper.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}
