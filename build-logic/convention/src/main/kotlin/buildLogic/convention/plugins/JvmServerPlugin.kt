package buildLogic.convention.plugins

import buildLogic.convention.configurations.DockerComposeConfiguration.Service
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Companion.APP_SERVICE_TYPE
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Companion.IDENTIFIER_KEY
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Companion.METRICS_PORT_KEY
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Companion.TYPE_KEY
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Companion.identifier
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Condition
import buildLogic.convention.configurations.DockerComposeConfiguration.Service.Condition.ConditionType
import buildLogic.convention.extensions.*
import buildLogic.convention.extensions.plugins.JvmServerExtension
import buildLogic.convention.tasks.CreateDockerComposeConfigTask
import com.avast.gradle.dockercompose.ComposeExtension
import com.avast.gradle.dockercompose.DockerComposePlugin
import io.github.gerardorodriguezdev.chamaleon.gradle.plugin.ChamaleonGradlePlugin
import io.github.gerardorodriguezdev.chamaleon.gradle.plugin.extensions.ChamaleonExtension
import io.ktor.plugin.*
import io.ktor.plugin.features.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

class JvmServerPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            applyPlugins()
            val chamaleonExtension = extensions.findByType(ChamaleonExtension::class.java)
            val jvmServerExtension = createJvmServerExtension()
            configureJavaApplicationExtension(jvmServerExtension)
            configureKtorExtension(chamaleonExtension, jvmServerExtension)
            configureKotlinExtension(jvmServerExtension)
            configureDockerComposeExtension(jvmServerExtension)
            registerTasks(jvmServerExtension, chamaleonExtension)
        }
    }

    private fun Project.applyPlugins() {
        pluginManager.apply {
            apply(KtorGradlePlugin::class.java)
            apply(KotlinPluginWrapper::class.java)
            apply(ChamaleonGradlePlugin::class.java)
            apply(DockerComposePlugin::class.java)
        }
    }

    private fun Project.createJvmServerExtension(): JvmServerExtension {
        val extension = extensions.create(JVM_SERVER_EXTENSION_NAME, JvmServerExtension::class.java)
        return extension
    }

    private fun Project.configureJavaApplicationExtension(jvmServerExtension: JvmServerExtension) {
        extensions.configure(JavaApplication::class.java) {
            mainClass.set(jvmServerExtension.mainClass)
        }
    }

    private fun Project.configureKtorExtension(
        chamaleonExtension: ChamaleonExtension?,
        jvmServerExtension: JvmServerExtension
    ) {
        val ktorExtension = extensions.getByType(KtorExtension::class.java)
        val ktorExtensions = (ktorExtension as ExtensionAware).extensions
        val dockerImageRegistry = DockerImageRegistry.externalRegistry(
            project = fullNameDockerImage(
                imageRegistryUrl = jvmServerExtension.dockerConfiguration.registryUrl,
                imageName = jvmServerExtension.dockerConfiguration.name,
                identifier = provider { "latest" },
            ),
            username = jvmServerExtension.dockerConfiguration.registryUsername,
            password = jvmServerExtension.dockerConfiguration.registryPassword,
        )
        val environmentVariablesProvider = provider { chamaleonExtension.toMap() }

        ktorExtensions.configure(DockerExtension::class.java) {
            localImageName.set(jvmServerExtension.dockerConfiguration.name)
            jreVersion.set(jvmServerExtension.jvmTarget.toJavaVersion())
            imageTag.set(jvmServerExtension.dockerConfiguration.tag)
            externalRegistry.set(dockerImageRegistry)
            customBaseImage.set(
                provider {
                    val jvmTarget = jvmServerExtension.jvmTarget.get()
                    baseDockerImage(jvmTarget)
                }
            )

            environmentVariablesProvider.get().forEach { (key, value) ->
                environmentVariables.add(
                    DockerEnvironmentVariable(
                        variable = key,
                        value = value,
                    )
                )
            }
        }
    }

    private fun Project.configureKotlinExtension(jvmServerExtension: JvmServerExtension) {
        extensions.configure(KotlinJvmProjectExtension::class.java) {
            compilerOptions {
                extraWarnings.set(true)
            }

            jvmToolchain {
                languageVersion.set(jvmServerExtension.jvmTarget.toJavaLanguageVersion())
            }
        }
    }

    private fun Project.configureDockerComposeExtension(jvmServerExtension: JvmServerExtension) {
        val dockerComposeDirectory = dockerComposeDirectory()
        extensions.configure(ComposeExtension::class.java) {
            useComposeFiles.add(
                dockerComposeDirectory.get()
                    .file(CreateDockerComposeConfigTask.DOCKER_COMPOSE_FILE_NAME).asFile.absolutePath
            )

            executable.set(jvmServerExtension.dockerComposeConfiguration.executablePath)
            dockerExecutable.set(jvmServerExtension.dockerConfiguration.executablePath)
        }
    }

    private fun Project.registerTasks(jvmServerExtension: JvmServerExtension, chamaleonExtension: ChamaleonExtension?) {
        val wasmWebsiteConsumerConfiguration =
            configurations.create(WasmWebsitePlugin.WASM_WEBSITE_CONSUMER_CONFIGURATION_NAME) {
                isCanBeConsumed = false
                isCanBeResolved = true
            }

        val syncWasmWebsiteFilesTask = tasks.register<Sync>(SYNC_WASM_WEBSITE_FILES_TASK_NAME) {
            from(wasmWebsiteConsumerConfiguration)
            into(layout.buildDirectory.dir("resources/main/static"))
        }

        tasks.named(PROCESS_RESOURCES_TASK_NAME) {
            dependsOn(syncWasmWebsiteFilesTask)
        }

        val environmentVariablesProvider = provider { chamaleonExtension.toMap() }
        tasks.named<JavaExec>(RUN_TASK_NAME) {
            dependsOn(tasks.named<Jar>(JVM_JAR_TASK_NAME))
            classpath(tasks.named<Jar>(JVM_JAR_TASK_NAME))

            environmentVariablesProvider.get().forEach { (key, value) ->
                environment(key, value)
            }

            systemProperty("io.ktor.development", jvmServerExtension.isDevelopment.get().toString())
        }

        val dockerComposeDirectory = dockerComposeDirectory()
        val createDockerComposeConfigTask =
            tasks.register<CreateDockerComposeConfigTask>(CREATE_DOCKER_COMPOSE_TASK_NAME) {
                val imageName = jvmServerExtension.dockerConfiguration.name.get()
                val imageTag = jvmServerExtension.dockerConfiguration.tag.get()
                val port = jvmServerExtension.dockerConfiguration.port.get()
                jvmServerExtension.dockerConfiguration.healthzPort.get()
                val metricsPort = jvmServerExtension.dockerConfiguration.metricsPort.get()
                val services =
                    jvmServerExtension.dockerComposeConfiguration.services.get()

                val appService = Service(
                    image = "$imageName:$imageTag",
                    ports = buildList {
                        add("$port:$port")
                    },
                    environment = environmentVariablesProvider.get(),
                    dependsOn = buildMap {
                        services.forEach { service ->
                            val identifier = service.identifier

                            val condition =
                                if (service.healthcheck != null) {
                                    ConditionType.SERVICE_HEALTHY
                                } else {
                                    ConditionType.SERVICE_STARTED
                                }

                            put(identifier, Condition(condition))
                        }
                    },
                    extras = mapOf(
                        IDENTIFIER_KEY to imageName,
                        TYPE_KEY to APP_SERVICE_TYPE,
                        METRICS_PORT_KEY to metricsPort.toString(),
                    )
                )

                servicesList.set(services + appService)
                outputDirectory.set(dockerComposeDirectory)
            }

        val buildImageTask = tasks.named(BUILD_DOCKER_IMAGE_TASK_NAME)
        val loadImageTask = tasks.register<Exec>(LOAD_DOCKER_IMAGE_TASK_NAME) {
            dependsOn(buildImageTask)
            dependsOn(createDockerComposeConfigTask)

            commandLine("bash", "-c", "docker load < build/jib-image.tar")
        }

        tasks.named(COMPOSE_BUILD_TASK_NAME) {
            dependsOn(loadImageTask)
        }
    }

    private fun Project.dockerComposeDirectory(): Provider<Directory> =
        project.layout.buildDirectory.dir(DOCKER_COMPOSE_DIRECTORY_NAME)

    private companion object {
        const val BUILD_DOCKER_IMAGE_TASK_NAME = "buildImage"
        const val LOAD_DOCKER_IMAGE_TASK_NAME = "loadImage"
        const val COMPOSE_BUILD_TASK_NAME = "composeBuild"
        const val RUN_TASK_NAME = "run"
        const val JVM_JAR_TASK_NAME = "jar"
        const val CREATE_DOCKER_COMPOSE_TASK_NAME = "createDockerCompose"
        const val SYNC_WASM_WEBSITE_FILES_TASK_NAME = "syncWasmWebsiteFiles"
        const val PROCESS_RESOURCES_TASK_NAME = "processResources"

        const val DOCKER_COMPOSE_DIRECTORY_NAME = "dockerCompose"

        const val JVM_SERVER_EXTENSION_NAME = "jvmServer"
    }
}
