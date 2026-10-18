// Standalone project: run Gradle from inside sift-farlands/ (e.g. ./gradlew build).
// It sits inside the The Sift workspace only for convenience and is not part of
// that project's build.
pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		mavenCentral()
		gradlePluginPortal()
	}
}

rootProject.name = "sift-farlands"
