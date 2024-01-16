package kim.nzxy.spel.json

import com.google.common.collect.Lists
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.stream.Collectors

/**
 * @author ly-chn
 * @since 2024/1/5 11:14
 */
object ConfigJsonUtil {
    const val FILENAME = "spel-extension.json"
    private val gson = Gson()
    fun isSpELFilename(filename: String): Boolean {
        return filename == FILENAME
    }

    fun isPropertyKey(element: PsiElement): Boolean {
        var sibling = element.parent
        while ((sibling.prevSibling.also { sibling = it }) != null) {
            if (":" == sibling.text) return false
        }
        return true
    }

    fun getParentNames(property: JsonProperty): String {
        val parentProperties = PsiTreeUtil.collectParents(
            property,
            JsonProperty::class.java, false
        ) { e: PsiElement? -> e is JsonFile }

        return Lists.reverse(parentProperties).stream()
            .map { p: JsonProperty -> p.name }
            .collect(Collectors.joining("."))
    }

    fun parseSpELInfo(text: CharSequence): Map<String, SpELInfo>? {
        return try {
            gson.fromJson<Map<String, SpELInfo>>(text.toString(), object : TypeToken<Map<String, SpELInfo>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }

    fun relaxMatch(prefix: String, text: String): Boolean {
        if (text.isEmpty() || prefix.isEmpty()) {
            return false
        }
        if (!StringUtil.charsEqualIgnoreCase(prefix[0], text[0])) {
            return false
        }
        var index = 0
        for (element in prefix) {
            val found = text.indexOf(element, index)
            if (found < 0) return false
            index = found + 1
        }
        return true
    }
}