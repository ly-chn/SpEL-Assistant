package kim.nzxy.spel.yaml

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.microservices.jvm.config.ConfigPlaceholderReference
import com.intellij.microservices.jvm.config.MicroservicesConfigBundle
import com.intellij.microservices.jvm.config.MicroservicesConfigUtils
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.text.CharFilter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import java.util.function.Function

/**
 * @author ly-chn
 * @since 2023/12/27 9:29
 * @see com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
 */
object ConfigYamlUtil {

    private val RELEVANT_REFERENCE_CONDITION =
        Condition { reference: PsiReference? -> reference !is ConfigPlaceholderReference }

    fun getParentKeyValue(element: PsiElement, originalElement: PsiElement?): YAMLKeyValue? {
        var parentYamlKeyValue = PsiTreeUtil.getParentOfType(originalElement, YAMLKeyValue::class.java)
        if (parentYamlKeyValue == null) {
            parentYamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
        }
        if (element.node.elementType === YAMLTokenTypes.SCALAR_KEY) {
            parentYamlKeyValue = PsiTreeUtil.getParentOfType(parentYamlKeyValue, YAMLKeyValue::class.java)
        }
        return parentYamlKeyValue
    }

    fun getQualifiedConfigKeyName(yamlKeyValue: YAMLKeyValue?): String {
        return getQualifiedConfigKeyName(yamlKeyValue) { obj: YAMLKeyValue -> obj.keyText }
    }

    fun matchPrefix(prefix: String, text: String): Boolean {
        if (text.isEmpty() || prefix.isEmpty()) {
            return false
        }
        if (!StringUtil.charsEqualIgnoreCase(prefix[0], text[0])) {
            return false
        }
        if (prefix == text) {
            return false
        }
        return StringUtil.startsWith(toUniform(text), toUniform(prefix))
    }

    fun isInsideSpELYaml(element: PsiElement?): Boolean {
        val containingFile = element?.containingFile ?: return false
        return containingFile.originalFile.name == "spel-extension.yml"
    }

    fun isConfigKeyPath(reference: PsiReference?): Boolean {
        return reference != null && reference.rangeInElement.isEmpty
    }

    fun highlightValueReferences(valueElement: PsiElement, holder: ProblemsHolder) {
        val valueReferences =
            ContainerUtil.filter(valueElement.references, RELEVANT_REFERENCE_CONDITION)
        val unresolvedReferences: MultiMap<Int, PsiReference> = MultiMap()
        val resolvedReferencesOffsets: MutableSet<Int?> = HashSet()
        var javaClassReferenceExtendsCandidate: JavaClassReference? = null
        for (reference in valueReferences) {
            val startOffset = reference.rangeInElement.startOffset
            val unresolved =
                if (reference is PsiPolyVariantReference) reference.multiResolve(false).isEmpty()
                else reference.resolve() == null
            if (!unresolved) {
                unresolvedReferences.remove(startOffset)
                resolvedReferencesOffsets.add(startOffset)
                if (reference is JavaClassReference) {
                    javaClassReferenceExtendsCandidate = reference
                }
            } else if (!resolvedReferencesOffsets.contains(startOffset)) {
                unresolvedReferences.putValue(startOffset, reference)
            }
        }

        if (javaClassReferenceExtendsCandidate != null) {
            highlightJavaClassReferenceExtends(holder, javaClassReferenceExtendsCandidate)
        }
        for (reference in unresolvedReferences.values()) {
            if (!reference.isSoft) {
                holder.registerProblem(
                    reference,
                    ProblemsHolder.unresolvedReferenceMessage(reference),
                    ProblemHighlightType.ERROR
                )
            }
        }
    }

    private fun highlightJavaClassReferenceExtends(holder: ProblemsHolder, reference: JavaClassReference) {
        val extendClassNames = reference.superClasses
        if (!extendClassNames.isEmpty()) {
            val resolve = reference.resolve()!!
            if (resolve is PsiClass) {
                val var4: Iterator<*> = extendClassNames.iterator()

                var extend: String?
                do {
                    if (!var4.hasNext()) {
                        holder.registerProblem(
                            reference.element,
                            MicroservicesConfigBundle.message(
                                "config.non.assignable.class", *arrayOf<Any>(
                                    ElementManipulators.getValueText(reference.element),
                                    StringUtil.join(extendClassNames, "|")
                                )
                            ),
                            ProblemHighlightType.ERROR,
                            ElementManipulators.getValueTextRange(reference.element)
                        )
                        return
                    }

                    extend = var4.next() as String?
                } while (!InheritanceUtil.isInheritor(
                        resolve as PsiClass,
                        extend!!
                    )
                )
            }
        }
    }

    private fun toUniform(input: String): String {
        val lowerCase = StringUtil.toLowerCase(input)
        return if (StringUtil.isLatinAlphanumeric(lowerCase)) {
            lowerCase
        } else {
            StringUtil.strip(lowerCase, UNIFORM_CHAR_FILTER)
        }
    }

    private fun getValuePresentationText(yamlKeyValue: YAMLKeyValue): String {
        when (val value = yamlKeyValue.value) {
            is YAMLScalar -> {
                return value.textValue
            }

            is YAMLSequence -> {
                val sequenceItems = value.items
                val suffix = if (sequenceItems.size > 2) ", [...]" else ""
                return StringUtil.join(
                    ContainerUtil.getFirstItems(sequenceItems, 2),
                    { item: YAMLSequenceItem ->
                        val itemValue = item.value
                        if (itemValue is YAMLScalar) itemValue.textValue
                        else getSequenceItemText(item)
                    }, ", "
                ) + suffix
            }

            else -> {
                return yamlKeyValue.valueText
            }
        }
    }

    private fun getQualifiedConfigKeyName(
        yamlKeyValue: YAMLKeyValue?,
        keyTextMapper: Function<YAMLKeyValue, String>
    ): String {
        val builder = StringBuilder()
        var element = yamlKeyValue

        var parent: YAMLKeyValue?
        var addSeparator = false
        while (element != null) {
            if (addSeparator) {
                builder.insert(0, '.')
            }
            builder.insert(0, keyTextMapper.apply(element))
            addSeparator = true
            parent = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java, true, YAMLSequenceItem::class.java)
            if (parent == null) {
                val item = PsiTreeUtil.getParentOfType(
                    element,
                    YAMLSequenceItem::class.java
                )
                if (item != null) {
                    val sequence = PsiTreeUtil.getParentOfType(
                        item,
                        YAMLSequence::class.java
                    )
                    if (sequence != null) {
                        val index = sequence.items.indexOf(item)
                        if (index >= 0) {
                            builder.insert(0, "[$index].")
                            addSeparator = false
                        }
                    }

                    parent = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)!!
                }
            }
            element = parent
        }
        return builder.toString()
    }

    private fun getSequenceItemText(item: YAMLSequenceItem): String {
        val keysValues: MutableList<YAMLKeyValue?> = SmartList()
        var count = 0
        var suffix = ""
        val iterator: Iterator<YAMLKeyValue> = item.keysValues.iterator()

        while (iterator.hasNext()) {
            keysValues.add(iterator.next())
            if (count++ == 2) {
                if (iterator.hasNext()) {
                    suffix = ", [...]"
                }
                break
            }
        }
        return StringUtil.join(
            keysValues,
            { keyValue -> keyValue!!.keyText + ": " + getValuePresentationText(keyValue) },
            ", "
        ) + suffix
    }

    private val UNIFORM_CHAR_FILTER =
        CharFilter { ch: Char -> Character.isLetterOrDigit(ch) || ch == '.' }

}