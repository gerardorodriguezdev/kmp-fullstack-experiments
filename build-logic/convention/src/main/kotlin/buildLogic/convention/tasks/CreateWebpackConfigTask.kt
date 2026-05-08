package buildLogic.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CreateWebpackConfigTask : DefaultTask() {

    @get:Input
    abstract val ignoredFiles: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun createFile() {
        val ignoredFiles = ignoredFiles.get()
        if (ignoredFiles.isEmpty()) return

        val ignoredFilesString = ignoredFiles.toIgnoredFilesString()
        val outputFile = outputFile.get().asFile
        outputFile.writeText(
            webpackConfigContent(ignoredFilesString = ignoredFilesString)
        )
    }

    private fun webpackConfigContent(ignoredFilesString: String): String =
        //language=javascript
        """
            const HtmlWebpackPlugin = require('html-webpack-plugin');
            
            config.plugins.push(
                new HtmlWebpackPlugin({
                    template: 'kotlin/template.html'
                })
            );
            
            if (config.devServer) {
                const directories = config.devServer.static || []

                config.devServer = {
                    ...config.devServer,
                    static: directories.map(item => {
                        const base = (typeof item === 'string')
                                ? { directory: item }
                                : { ...item };
                            return {
                                ...base,
                                watch: {
                                    ignored: [$ignoredFilesString],
                                    usePolling: false
                                }
                            };
                    })
                }
            }
            
            if (config.mode === 'production') {
                const CompressionPlugin = require('compression-webpack-plugin');
                const zlib = require('node:zlib');
                
                config.plugins.push(
                    new CompressionPlugin({
                        filename: '[path].br',
                        algorithm: 'brotliCompress',
                        test: /\.(js|css|html|svg|wasm)$/,
                        compressionOptions: {
                            params: {
                                [zlib.constants.BROTLI_PARAM_QUALITY]: 11
                            }
                        },
                        deleteOriginalAssets: false
                    })
                );
            }
        """.trimIndent()

    private fun List<String>.toIgnoredFilesString(): String =
        joinToString(separator = ",", transform = { "'$it'" })
}
