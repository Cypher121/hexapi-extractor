plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "8.1.0"
}

group = "coffee.cypher"
version = "0.0.1"

repositories {
    mavenCentral()
    maven(url = "https://maven.blamejared.com")
}

dependencies {
    implementation("org.javassist:javassist:3.29.2-GA")
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation("com.googlecode.json-simple:json-simple:1.1.1") {
        exclude(module = "junit")
    }

    compileOnly("at.petra-k.paucal:paucal-common-1.19.2:0.5.0")
    compileOnly("at.petra-k.hexcasting:hexcasting-common-1.19.2:0.10.3")
    compileOnly("vazkii.patchouli:Patchouli-xplat:1.19.2-77")
    compileOnly("org.ow2.asm:asm:9.4")
    compileOnly("com.google.code.gson:gson:2.8.9")
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        manifest {
            attributes(
                "Premain-Class" to "coffee.cypher.hexapi_extractor.AgentKt",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
            )
        }
    }

    shadowJar {
        relocate("org.javassist", "coffee.cypher.shadowed.org.javassist")
        relocate("org.jetbrains", "coffee.cypher.shadowed.org.jetbrains")
        relocate("org.json.simple", "coffee.cypher.shadowed.org.json.simple")
    }
}

kotlin {
    jvmToolchain(17)
}