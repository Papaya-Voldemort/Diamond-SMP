dependencies {
    api(project(":core-api"))
    implementation(project(":core-domain"))
    implementation(project(":core-service"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}
