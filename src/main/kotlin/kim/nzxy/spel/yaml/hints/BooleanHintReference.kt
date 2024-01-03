package kim.nzxy.spel.yaml.hints

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.annotations.NonNls

/**
 * @author ly-chn
 * @since 2024/1/3 11:13
 */
internal open class BooleanHintReference(element: PsiElement, rangeInElement: TextRange?) :
    PsiReferenceBase<PsiElement?>(element, rangeInElement), EmptyResolveMessageProvider {
    private val values = arrayOf("true", "false")
    override fun resolve(): PsiElement? {
        return this.element
    }

    override fun getUnresolvedMessagePattern(): @NonNls String {
        return "Invalid value, must be one of false | true"
    }

    override fun getVariants(): Array<String> {
        return values
    }
}