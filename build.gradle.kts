plugins {
	// 26.x uses the new remap-less plugin id. The old `fabric-loom` id is the
	// remapping loom and fails on unobfuscated versions with
	// "Configuration 'mappings' has no dependencies".
	id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
	java
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}

repositories {
	maven("https://maven.fabricmc.net/") { name = "Fabric" }
	mavenCentral()
}

dependencies {
	// Minecraft 26.3 is shipped unobfuscated, so no `mappings` dependency is
	// needed when using the remap-less loom plugin.
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
}

java {
	withSourcesJar()
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}

tasks.processResources {
	inputs.property("version", project.version)
	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName.get()}" }
	}
}
