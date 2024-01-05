package kim.nzxy.spel.json

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList

/**
 * @author ly-chn
 * @since 2024/1/5 11:23
 */
class SpELJsonCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position as? LeafPsiElement ?: return
        if (parameters.originalFile !is JsonFile || ConfigJsonUtil.isSpELFilename(parameters.originalFile.name)) return
        if (parameters.completionType != CompletionType.BASIC) return

        val parent = position.parent
        if (parent is JsonStringLiteral || parent is JsonReferenceExpression) {
            val parentParent = parent.parent as? JsonProperty ?: return
            handleJsonProperty(
                position,
                parentParent, result
            )
        } else if (parent is JsonProperty) {
            handleJsonProperty(
                position,
                parent, result
            )
        }
    }

    private fun handleJsonProperty(element: PsiElement, property: JsonProperty, result: CompletionResultSet) {
        if (!isPropertyKey(element)) return
        val presentNamePart: String = ConfigJsonUtil.getParentNames(property)
        if (presentNamePart.isNotEmpty()) {
            return
        }

        val lookupElements = SmartList<LookupElement>()
        val service = JsonSuggestionService.getInstance()
        val configKeys = service.getAllMetaConfigKeys(element.project)
        for (configKey in configKeys) {
            result.addElement(
                LookupElementBuilder.create(configKey)
                    .withInsertHandler(SpELInsertHandler)
            )
        }
        result.addAllElements(lookupElements)
    }

    private fun isPropertyKey(element: PsiElement): Boolean {
        var sibling = element.parent
        while ((sibling.prevSibling.also { sibling = it }) != null) {
            if (":" == sibling.text) return false
        }
        return true
    }

    object SpELInsertHandler: InsertHandler<LookupElement>{
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            context.document.deleteString(context.startOffset, context.tailOffset)
            val quoted = """"${item.lookupString}": {
                    |"method": {
                    |"result": false,
                    |"resultName": "result",
                    |"parameters": false,
                    |"parametersPrefix": ["p", "a"],
                    |},
                    |"fields": {
                    |"demo": "java.util.Map<String, String>"
                    |}},""".trimMargin()
            context.document.insertString(context.startOffset, quoted)
            context.commitDocument()
            CodeStyleManagerImpl.getInstance(context.project)
                .reformatText(context.file, context.startOffset, context.startOffset + quoted.length)
        }
    }
}