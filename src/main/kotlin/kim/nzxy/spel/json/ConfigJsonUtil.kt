package kim.nzxy.spel.json

import com.google.common.collect.Lists
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.stream.Collectors

/**
 * @author ly-chn
 * @since 2024/1/5 11:14
 */
object ConfigJsonUtil {
    private val gson = Gson()
    fun isSpELFilename(filename: String): Boolean {
        return filename == "spel-extension.json"
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

}