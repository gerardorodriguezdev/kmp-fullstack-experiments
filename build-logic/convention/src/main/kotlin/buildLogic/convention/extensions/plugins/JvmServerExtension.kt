package buildLogic.convention.extensions.plugins

import buildLogic.convention.builders.ServicesBuilder
import buildLogic.convention.builders.ServicesBuilderScope
import buildLogic.convention.configurations.DockerComposeConfiguration
import buildLogic.convention.configurations.DockerConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

open class JvmServerExtension @Inject constructor(
    objects: ObjectFactory,
    private val providerFactory: ProviderFactory
) {
    val jvmTarget: Property<Int> = objects.property(Int::class.java)
    val mainClass: Property<String> = objects.property(String::class.java)
    val isDevelopment: Property<Boolean> = objects.property(Boolean::class.java)

    val dockerConfiguration: DockerConfiguration = objects.newInstance(DockerConfiguration::class)
    val dockerComposeConfiguration: DockerComposeConfiguration = objects.newInstance(DockerComposeConfiguration::class)

    fun dockerConfiguration(configure: DockerConfiguration.() -> Unit) {
        dockerConfiguration.configure()
    }

    fun dockerComposeConfiguration(configure: DockerComposeConfiguration.() -> Unit) {
        dockerComposeConfiguration.configure()
    }

    fun DockerComposeConfiguration.images(scope: ServicesBuilderScope.() -> Unit = {}) {
        services.addAll(
            providerFactory.provider {
                ServicesBuilder().apply(scope).services
            }
        )
    }
}
