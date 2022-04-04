val jdk16Home = when {
    project.hasProperty("JDK_16") -> project.property("JDK_16")
    System.getenv("JDK_16") != null -> System.getenv("JDK_16")
    else -> throw GradleException("JDK_16 environment variable is not defined")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_6
    targetCompatibility = JavaVersion.VERSION_1_6
}

tasks.withType<JavaCompile> {
    options.isFork = true
    options.forkOptions.executable = "$jdk16Home/bin/javac"
}

dependencies {
    implementation(files("$jdk16Home/lib/tools.jar"))
    implementation(files("$jdk16Home/lib/sa-jdi.jar"))
}
