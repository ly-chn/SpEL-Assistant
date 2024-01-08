package kim.nzxy.spel.json

import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.spring.SpringApiIcons

/**
 * @author ly-chn
 * @since 2024/1/8 14:17
 */
object SpELExtensionFileType : LanguageFileType(JsonLanguage.INSTANCE, true), FileTypeIdentifiableByVirtualFile {
    val INSTANCE: SpELExtensionFileType = SpELExtensionFileType

    override fun getName() = "spel-extension.json"

    override fun getDescription() = "spel-extension.json"

    override fun getDefaultExtension() = "json"

    override fun getIcon() = SpringApiIcons.Spring

    // todo in resource fold
    override fun isMyFileType(virtualFile: VirtualFile) = ConfigJsonUtil.isSpELFilename(virtualFile.name)
}