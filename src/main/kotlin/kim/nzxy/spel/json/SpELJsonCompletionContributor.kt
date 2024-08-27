package kim.nzxy.spel.json

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.components.service
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import kim.nzxy.spel.service.JsonSuggestionService

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
        if (!ConfigJsonUtil.isPropertyKey(element)) return
        val presentNamePart: String = ConfigJsonUtil.getParentNames(property)
        if (presentNamePart.isNotEmpty()) {
            return
        }
        val existedKeys = property.parent.children.filter { it is JsonProperty && it != property }
            .map { (it as JsonProperty).name }
        val service = element.project.service<JsonSuggestionService>()
        val configKeys = service.getAllMetaConfigKeys()
        var text = CompletionUtil.getOriginalElement(element)?.text ?:return
        text = StringUtil.unquoteString(text)
        for (configKey in configKeys) {
            if (!existedKeys.contains(configKey) && ConfigJsonUtil.relaxMatch(text, configKey)) {
                result.addElement(LookupElementBuilder.create(configKey).withInsertHandler(SpELInsertHandler))
            }
        }
    }

    object SpELInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val element = context.file.findElementAt(context.startOffset) ?: return
            val literal = PsiTreeUtil.getParentOfType(element, JsonStringLiteral::class.java, false)
            val range = literal?.textRange ?: element.textRange
            var hasBody = false
            val hasQuote = literal != null
            if (literal != null) {
                val next = PsiTreeUtil.nextLeaf(literal)
                if (next != null && next !is PsiErrorElement) {
                    hasBody = true
                }
            }
            val quoted = if (hasBody) {
                "\"${item.lookupString}\""
            } else {
                """"${item.lookupString}": {"prefix": "", "suffix": "", "method": {"result": false,"resultName": "result","parameters": false,
                    "parametersPrefix": ["p", "a"],},"fields": {"demo": "java.util.Map<String, String>"}},"""
            }
            val endOffSet = if (hasBody || hasQuote) range.endOffset else range.startOffset + item.lookupString.length
            // 引号内, 引号外, 后面有字符串, 后面没有字符串, 有body, 无body
            context.document.replaceString(range.startOffset, endOffSet, quoted)
            context.commitDocument()
            CodeStyleManager.getInstance(context.project)
                .reformatText(context.file, range.startOffset, range.startOffset + quoted.length)
        }
    }
}