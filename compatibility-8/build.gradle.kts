val jdk18Home = when {
    project.hasProperty("JDK_18") -> project.property("JDK_18")
    System.getenv("JDK_18") != null -> System.getenv("JDK_18")
    else -> throw GradleException("JDK_18 environment variable is not defined")
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.isFork = true
    options.forkOptions.executable = "$jdk18Home/bin/javac"
}

sourceSets {
    main {
        java.srcDirs("src")
    }
}

dependencies {
    implementation(project(":core")) {
        isTransitive = false // no need for parent jdk libs
    }
    implementation(files("$jdk18Home/lib/tools.jar"))
    implementation(files("$jdk18Home/lib/sa-jdi.jar"))
}
