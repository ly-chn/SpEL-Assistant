package kim.nzxy.spel.yaml

import com.intellij.openapi.util.text.CharFilter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem
import java.util.*
import java.util.function.Function

/**
 * @author ly-chn
 * @since 2023/12/27 9:29
 * @see com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
 */
object ConfigYamlUtil {

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

    private fun toUniform(input: String): String {
        val lowerCase = StringUtil.toLowerCase(input)
        return if (StringUtil.isLatinAlphanumeric(lowerCase)) {
            lowerCase
        } else {
            StringUtil.strip(lowerCase, UNIFORM_CHAR_FILTER)
        }
    }

    private val UNIFORM_CHAR_FILTER =
        CharFilter { ch: Char -> Character.isLetterOrDigit(ch) || ch == '.' }

}