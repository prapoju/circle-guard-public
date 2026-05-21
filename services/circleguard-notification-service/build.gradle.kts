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
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("com.twilio.sdk:twilio:10.1.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
