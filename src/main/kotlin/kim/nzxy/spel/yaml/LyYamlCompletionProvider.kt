package kim.nzxy.spel.yaml

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.util.ProcessingContext

class LyYamlCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val originalElement = CompletionUtil.getOriginalElement(element)
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val service = YamlSuggestionService.getInstance()
        val configKeys = service.getAllMetaConfigKeys(module)
        val parent = ConfigYamlUtil.getParentKeyValue(element, originalElement)
        val qualifiedConfigKeyName = ConfigYamlUtil.getQualifiedConfigKeyName(parent)

        configKeys
            .filter { qualifiedConfigKeyName.isEmpty() || ConfigYamlUtil.matchPrefix(qualifiedConfigKeyName, it) }
            .forEach {
            val decorator = LookupElementDecorator.withInsertHandler(
                LookupElementBuilder.create(it)
                    .withTypeText("SpEL", true)
                    .bold(),
                YamlKeyInsertHandler
            )
            result.addElement(decorator)
        }
    }
}
