
plugins {
    scala
    `java-library`
}

subprojects {

    apply(plugin = "scala")
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.scala-lang:scala-library:2.12.15")
        implementation("com.google.guava:guava:30.1.1-jre")
        api("ch.epfl.scala:bsp4j:2.0.0")

        implementation("ch.qos.logback:logback-classic:1.2.3")
        implementation("com.typesafe.scala-logging:scala-logging_2.12:3.9.4")

        implementation("org.gradle:gradle-tooling-api:7.3-20210825160000+0000")

        // scalaCompilerPlugins("org.typelevel:kind-projector_2.13.1:0.11.0")

        // for Bill
        implementation("org.scala-lang:scala-compiler:2.12.15")
        implementation("org.scalameta:scalameta_2.12:4.4.28")
        implementation("io.get-coursier:interface:1.0.6")
        implementation("org.scalameta:metals_2.12:0.10.9")

    }

    configurations.all {
        exclude(module = "scribe-slf4j_2.12")
    }

    testing {
        suites {
            // Configure the built-in test suite
            val test by getting(JvmTestSuite::class) {
                // Use JUnit4 test framework
                useJUnit("4.13.2")

                dependencies {
                    // Use Scalatest for testing our library
                    implementation("org.scalatest:scalatest_2.12:3.2.9")
                    implementation("org.scalatestplus:junit-4-13_2.12:3.2.2.0")

                    // Need scala-xml at test runtime
                    runtimeOnly("org.scala-lang.modules:scala-xml_2.12:1.2.0")
                }
            }
        }
    }
}
