package kim.nzxy.spel.json

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

/**
 * @author ly-chn
 * @since 2024/1/5 11:13
 */
class SpELJsonSchemaProviderFactory : JsonSchemaProviderFactory {
    private val themeSchemaPath = "/schemes/spel.schema.json"

    override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
        return listOf<JsonSchemaFileProvider>(object : JsonSchemaFileProvider {
            override fun isAvailable(file: VirtualFile) = ConfigJsonUtil.isSpELFilename(file.name)

            override fun getName() = "SpEL Extension"

            override fun getSchemaFile() = JsonSchemaProviderFactory.getResourceFile(javaClass, themeSchemaPath)

            override fun getSchemaType() = SchemaType.embeddedSchema
        })
    }
}