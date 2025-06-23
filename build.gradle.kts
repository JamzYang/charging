plugins {
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.23" apply false // 如需 Kotlin 支持可保留，否则可移除
}

group = "tech.dnacloud.charging"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "io.spring.dependency-management")
    group = rootProject.group!!
    version = rootProject.version!!

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
        }
    }
} 