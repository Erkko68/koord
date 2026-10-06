import io.github.erkko68.filament.filamat.MaterialBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.Base64
import javax.inject.Inject

// The camera image drawn behind the scene. The two platforms deliver it
// differently, so each gets its own way of reading a colour from it.

// Android: ARCore writes a GL_TEXTURE_EXTERNAL_OES texture, already RGB.
private const val SAMPLE_EXTERNAL = "texture(materialParams_cameraTexture, uv).rgb"

// iOS: ARKit delivers full-range BT.601 YCbCr in two planes, luma and chroma.
private const val SAMPLE_YCBCR = """
        mat4(
            vec4(1.0, 1.0, 1.0, 0.0),
            vec4(0.0, -0.3441, 1.7720, 0.0),
            vec4(1.4020, -0.7141, 0.0, 0.0),
            vec4(-0.7010, 0.5291, -0.8860, 1.0)
        ) * vec4(
            texture(materialParams_cameraTextureY, uv).r,
            texture(materialParams_cameraTextureCbCr, uv).rg,
            1.0
        )"""

private fun fragment(sample: String) = """
    void material(inout MaterialInputs material) {
        prepareMaterial(material);
        vec3 p = vec3(getNormalizedViewportCoord().xy, 1.0);
        vec2 uv = vec2(dot(materialParams.uvTransformU, p), dot(materialParams.uvTransformV, p));
        vec3 color = ($sample).rgb;
        // The camera image is already display-referred; undo the tone mapping
        // the view applies to everything it draws.
        material.baseColor.rgb = inverseTonemapSRGB(color);
        material.baseColor.a = 1.0;
    }
"""

/**
 * Compiles the camera background material with filamat and writes it to
 * [outputDir] as a Kotlin constant, `CAMERA_BACKGROUND_FILAMAT_BASE64`: for
 * Android (OpenGL, an external texture) or, with [yCbCr], for iOS (Metal, a
 * luma and a chroma texture).
 *
 * filamat comes from the filament-kmp release the library is built against,
 * so the material always matches the Filament that loads it.
 */
@CacheableTask
abstract class GenerateCameraBackgroundMaterial : DefaultTask() {
    /** filamat for the JVM, with its native library. */
    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    @get:Input
    abstract val yCbCr: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val workers: WorkerExecutor

    @TaskAction
    fun generate() {
        // In its own process: filamat loads a native library, which must not end up in the Gradle daemon.
        workers.processIsolation { classpath.from(compilerClasspath) }
            .submit(Compile::class.java) {
                yCbCr.set(this@GenerateCameraBackgroundMaterial.yCbCr)
                outputDir.set(this@GenerateCameraBackgroundMaterial.outputDir)
            }
    }

    interface Parameters : WorkParameters {
        val yCbCr: Property<Boolean>
        val outputDir: DirectoryProperty
    }

    abstract class Compile : WorkAction<Parameters> {
        override fun execute() {
            val yCbCr = parameters.yCbCr.get()
            MaterialBuilder.init()
            val material = try {
                MaterialBuilder()
                    .name("KoordCameraBackground")
                    .shading(MaterialBuilder.Shading.UNLIT)
                    .vertexDomain(MaterialBuilder.VertexDomain.DEVICE)
                    .depthWrite(false)
                    .depthCulling(false)
                    .culling(MaterialBuilder.CullingMode.NONE)
                    // Rows of the affine map from viewport coordinates (0..1, origin
                    // bottom-left) to camera texture coordinates.
                    .parameter("uvTransformU", MaterialBuilder.UniformType.FLOAT3)
                    .parameter("uvTransformV", MaterialBuilder.UniformType.FLOAT3)
                    .platform(MaterialBuilder.Platform.MOBILE)
                    .apply {
                        if (yCbCr) {
                            parameter("cameraTextureY", MaterialBuilder.SamplerType.SAMPLER_2D)
                            parameter("cameraTextureCbCr", MaterialBuilder.SamplerType.SAMPLER_2D)
                            targetApi(MaterialBuilder.TargetApi.METAL)
                            material(fragment(SAMPLE_YCBCR))
                        } else {
                            parameter("cameraTexture", MaterialBuilder.SamplerType.SAMPLER_EXTERNAL)
                            targetApi(MaterialBuilder.TargetApi.OPENGL)
                            material(fragment(SAMPLE_EXTERNAL))
                        }
                    }
                    .build()
            } finally {
                MaterialBuilder.shutdown()
            }
            check(material.isValid) { "The camera background material failed to compile" }

            val base64 = Base64.getEncoder().encodeToString(material.data)
            parameters.outputDir.get().file("CameraBackgroundMaterial.kt").asFile.writeText(
                "package io.github.erkko68.koord.camera\n\n" +
                    "internal const val CAMERA_BACKGROUND_FILAMAT_BASE64 =\n" +
                    base64.chunked(100).joinToString(" +\n") { "    \"$it\"" } + "\n",
            )
        }
    }
}
