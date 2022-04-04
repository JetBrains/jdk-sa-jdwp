plugins {
    id("com.github.breadmoirai.github-release") version "2.2.12"
    `maven-publish`
}

version = "1.21"

allprojects {
    apply(plugin = "java")
}

val mainJar by tasks.registering(Jar::class) {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "com.jetbrains.sa.SaJdwp"
            )
        )
    }
    from(project(":core").the<SourceSetContainer>()["main"].output)
    from(project(":compatibility-8").the<SourceSetContainer>()["main"].output)
    from(project(":compatibility-10").the<SourceSetContainer>()["main"].output)
    from(project(":compatibility-13").the<SourceSetContainer>()["main"].output)
    archiveBaseName.set("sa-jdwp")
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project(":core").the<SourceSetContainer>()["main"].java)
    from(project(":compatibility-8").the<SourceSetContainer>()["main"].java)
    from(project(":compatibility-10").the<SourceSetContainer>()["main"].java)
    from(project(":compatibility-13").the<SourceSetContainer>()["main"].java)
    archiveBaseName.set("sa-jdwp-sources")
}

artifacts {
    add("archives", mainJar)
    add("archives", sourcesJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "sa-jdwp"
            groupId = "org.jetbrains.intellij.deps"
            version = project.version.toString()

            artifact(mainJar)
            artifact(sourcesJar)
        }
    }
    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            credentials {
                username = System.getenv("INTELLIJ_DEPENDENCIES_BOT")
                password = System.getenv("INTELLIJ_DEPENDENCIES_TOKEN")
            }
        }
    }
}

githubRelease {
    token(System.getenv("GITHUB_RELEASE_TOKEN")?.toString() ?: "")
    owner.set("jetbrains")
    repo.set("jdk-sa-jdwp")
}
