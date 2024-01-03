package kim.nzxy.spel.yaml

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.microservices.config.yaml.ConfigYamlAccessor
import com.intellij.spring.boot.application.metadata.SpringBootApplicationMetaConfigKeyManager
import org.jetbrains.yaml.completion.YamlKeyCompletionInsertHandler
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.util.*

/**
 * @author ly-chn
 * @since 2024/1/2 10:38
 */
object YamlKeyInsertHandler: YamlKeyCompletionInsertHandler<LookupElementDecorator<LookupElementBuilder>>() {
    override fun createNewEntry(
        document: YAMLDocument,
        item: LookupElementDecorator<LookupElementBuilder>,
        parent: YAMLKeyValue?
    ): YAMLKeyValue {
        val qualifiedKey = item.lookupString
        val accessor = LyConfigYamlAccessor(document)
        var keyValue = accessor.findExistingKey(qualifiedKey)
        if (keyValue != null) {
            return keyValue
        } else {
            keyValue = accessor.create(qualifiedKey)
            return keyValue!!
        }
    }

    override fun handleInsert(context: InsertionContext, item: LookupElementDecorator<LookupElementBuilder>) {
        super.handleInsert(context, item)
        AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
    }
}