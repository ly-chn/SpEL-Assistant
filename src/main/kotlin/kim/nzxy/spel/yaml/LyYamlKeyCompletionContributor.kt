package kim.nzxy.spel.yaml

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue

class LyYamlKeyCompletionContributor : CompletionContributor() {
    init {
        this.extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE)
                .inVirtualFile(
                    PlatformPatterns.virtualFile()
                        .withName("spel-extension.yml")
                        .ofType(YAMLFileType.YML)
                ),
            LyYamlCompletionProvider()
        )
    }
}