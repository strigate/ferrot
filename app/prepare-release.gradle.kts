import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

val releaseApkTargetFileNameFromBuild = extra["prepareReleaseApkFileName"] as String
val releaseAabTargetFileNameFromBuild = extra["prepareReleaseAabFileName"] as String

abstract class PrepareReleaseTask : DefaultTask() {
    @get:Input
    abstract val releaseArtifactsDirectoryPath: Property<String>

    @get:Input
    abstract val debugUnitTestReportPath: Property<String>

    @get:Input
    abstract val releaseApkSourcePath: Property<String>

    @get:Input
    abstract val releaseAabSourcePath: Property<String>

    @get:Input
    abstract val releaseMappingSourcePath: Property<String>

    @get:Input
    abstract val releaseApkTargetFileName: Property<String>

    @get:Input
    abstract val releaseAabTargetFileName: Property<String>

    @TaskAction
    fun prepareRelease() {
        val releaseDir = File(releaseArtifactsDirectoryPath.get())
        releaseDir.deleteRecursively()
        releaseDir.mkdirs()

        copyArtifact(
            sourcePath = releaseApkSourcePath.get(),
            targetPath = File(releaseDir, releaseApkTargetFileName.get()).absolutePath,
        )
        copyArtifact(
            sourcePath = releaseAabSourcePath.get(),
            targetPath = File(releaseDir, releaseAabTargetFileName.get()).absolutePath,
        )
        copyArtifact(
            sourcePath = releaseMappingSourcePath.get(),
            targetPath = File(releaseDir, "mapping.txt").absolutePath,
        )
        println()
        println("Release artifacts prepared in: ${releaseArtifactsDirectoryPath.get()}")
        println("Test report: ${debugUnitTestReportPath.get()}")
    }

    private fun copyArtifact(
        sourcePath: String,
        targetPath: String,
    ) {
        val sourceFile = File(sourcePath)
        check(sourceFile.exists()) {
            "Expected artifact was not found: $sourcePath"
        }
        sourceFile.copyTo(File(targetPath), overwrite = true)
    }
}

tasks.configureEach {
    val isCleanTask = name == "clean" || name.contains("Clean")
    if (!isCleanTask && name != "prepareRelease") {
        mustRunAfter("clean")
    }
    if (name.contains("Release") && !isCleanTask && name != "prepareRelease") {
        mustRunAfter("testDebugUnitTest")
    }
}

tasks.register("prepareRelease", PrepareReleaseTask::class.java) {
    group = "distribution"
    description = buildString {
        append("Cleans the build, runs debug unit tests, ")
        append("builds release APK and AAB and collects ")
        append("them into one directory.")
    }
    dependsOn(
        "clean",
        "testDebugUnitTest",
        "assembleRelease",
        "bundleRelease",
    )
    releaseArtifactsDirectoryPath.set(
        layout.buildDirectory.dir("outputs/release")
            .map { it.asFile.absolutePath },
    )
    debugUnitTestReportPath.set(
        layout.buildDirectory.file("reports/tests/testDebugUnitTest/index.html")
            .map { it.asFile.absolutePath },
    )
    releaseApkSourcePath.set(
        layout.buildDirectory.file("outputs/apk/release/app-release.apk")
            .map { it.asFile.absolutePath },
    )
    releaseAabSourcePath.set(
        layout.buildDirectory.file("outputs/bundle/release/app-release.aab")
            .map { it.asFile.absolutePath },
    )
    releaseMappingSourcePath.set(
        layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
            .map { it.asFile.absolutePath },
    )
    releaseApkTargetFileName.set(releaseApkTargetFileNameFromBuild)
    releaseAabTargetFileName.set(releaseAabTargetFileNameFromBuild)
}
