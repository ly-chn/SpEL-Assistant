package kim.nzxy.spel.yaml

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

class LyYamlKeyCompletionContributor : CompletionContributor() {


    init {
        val provider = object : CompletionProvider<CompletionParameters>() {
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
                val keyName = ConfigYamlUtil.getQualifiedConfigKeyName(parent)
                val accessor = LyConfigYamlAccessor(element)
                configKeys.forEach { (key, type) ->
                    if (keyName.isEmpty() ||
                        (ConfigYamlUtil.matchPrefix(keyName, key) && accessor.findExistingKey(key) == null)
                    ) {
                        val decorator = LookupElementDecorator.withInsertHandler(
                            LookupElementBuilder.create(key)
                                .withTypeText(type, true),
                            YamlKeyInsertHandler
                        )
                        result.addElement(decorator)
                    }
                }
                result.stopHere()
            }
        }

        val fileCapture = PlatformPatterns.psiElement().andNot(PlatformPatterns.psiComment())
            .inVirtualFile(PlatformPatterns.virtualFile().ofType(YAMLFileType.YML).withName("spel-extension.yml"))

        this.extend(CompletionType.BASIC, fileCapture.withSuperParent(2, YAMLMapping::class.java), provider)
        this.extend(CompletionType.BASIC, fileCapture.withSuperParent(2, YAMLDocument::class.java), provider)
        this.extend(
            CompletionType.BASIC,
            fileCapture.withParent(PlatformPatterns.psiElement(YAMLScalar::class.java))
                .afterLeaf(PlatformPatterns.psiElement(YAMLTokenTypes.INDENT)),
            provider
        )
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java)
                .withElementType(YAMLTokenTypes.SCALAR_KEY).withParent(YAMLKeyValue::class.java),
            provider
        )
    }
}