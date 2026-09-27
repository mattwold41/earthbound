plugins {
    java
}

group = "com.earthbound"
version = "0.1.0"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    implementation("com.twelvemonkeys.imageio:imageio-tiff:3.15.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        archiveBaseName.set("EarthBound")

        duplicatesStrategy =
            DuplicatesStrategy.EXCLUDE

        from(
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) {
                    it
                } else {
                    zipTree(it)
                }
            }
        )
    }
}
