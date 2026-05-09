plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    sourceSets {
        create("integrationTest") {
            compileClasspath += sourceSets["main"].output + sourceSets["test"].output
            runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
        }
    }
}

configurations {
    val integrationTestImplementation by getting {
        extendsFrom(configurations["testImplementation"])
    }
    val integrationTestRuntimeOnly by getting {
        extendsFrom(configurations["runtimeOnly"])
    }
}

tasks.named<Test>("integrationTest") {
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath + sourceSets["main"].runtimeClasspath
    shouldRunAfter("test")
    outputs.upToDateWhen { false }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
