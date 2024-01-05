package kim.nzxy.spel.json

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * @author ly-chn
 * @since 2024/1/5 11:23
 */
class SpELJsonCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position as? LeafPsiElement ?: return
        if (parameters.originalFile !is JsonFile || !ConfigJsonUtil.isSpELFilename(parameters.originalFile.name)) return
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
        val existedKeys = property.parent.children.filter { it is JsonProperty && it != property }
            .map { (it as JsonProperty).name }
        val service = JsonSuggestionService.getInstance()
        val configKeys = service.getAllMetaConfigKeys(element.project)
        for (configKey in configKeys) {
            if (!existedKeys.contains(configKey)) {
                result.addElement(LookupElementBuilder.create(configKey).withInsertHandler(SpELInsertHandler))
            }
        }
    }

    private fun isPropertyKey(element: PsiElement): Boolean {
        var sibling = element.parent
        while ((sibling.prevSibling.also { sibling = it }) != null) {
            if (":" == sibling.text) return false
        }
        return true
    }

    object SpELInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val element = context.file.findElementAt(context.startOffset) ?: return
            val literal = PsiTreeUtil.getParentOfType(element, JsonStringLiteral::class.java, false)
            val range = PsiTreeUtil.getParentOfType(element, JsonStringLiteral::class.java, false)?.textRange
                ?: element.textRange
            var hasBody = false
            if (literal != null) {
                val next = PsiTreeUtil.nextLeaf(literal)
                if (next != null && next !is PsiErrorElement) {
                    hasBody = true
                }
            }
            val quoted = if (hasBody) {
                "\"${item.lookupString}\""
            } else {
                """"${item.lookupString}": {"method": {"result": false,"resultName": "result","parameters": false,
                    "parametersPrefix": ["p", "a"],},"fields": {"demo": "java.util.Map<String, String>"}},"""
            }
            context.document.replaceString(range.startOffset, range.endOffset, quoted)
            context.commitDocument()
            CodeStyleManager.getInstance(context.project)
                .reformatText(context.file, range.startOffset, range.startOffset + quoted.length)
        }
    }
}