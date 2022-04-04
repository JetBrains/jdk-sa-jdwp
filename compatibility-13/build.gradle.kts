val jdk13Home = when {
    project.hasProperty("JDK_13") -> project.property("JDK_13")
    System.getenv("JDK_13") != null -> System.getenv("JDK_13")
    else -> throw GradleException("JDK_13 environment variable is not defined")
}

java {
    targetCompatibility = JavaVersion.VERSION_13
    sourceCompatibility = JavaVersion.VERSION_13
}

tasks.withType<JavaCompile> {
    options.isFork = true
    options.forkOptions.executable = "$jdk13Home/bin/javac"
    options.compilerArgs = listOf(
        "--add-modules",
        "jdk.hotspot.agent",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.classfile=ALL-UNNAMED",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.memory=ALL-UNNAMED",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.runtime=ALL-UNNAMED",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
        "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
    )
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
}
