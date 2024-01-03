package kim.nzxy.spel.yaml

import org.intellij.lang.annotations.Language

/**
 * @author ly-chn
 * @since 2024/1/3 15:33
 */
data class YamlDocInfo(val type: String, val fullType: String, val defaultValue: String, val explain: String) {
    fun toHTML(title: String): String {
        @Language("HTML")
        val res = """
            <div class='definition'>
                <pre><b>$title</b><br><code style='font-size:90%;'>$fullType</code></pre>
            </div>
            <div class='content'>$explain<br><br></div>
            <table class='sections'>
                <tr>
                    <td valign='top' class='section'><p>Default:</td>
                    <td valign='top'>
                        <pre>$defaultValue</pre>
                    </td>
            </table>
        """.trimIndent()
        return res
    }
}
