package kim.nzxy.spel

object LyUtil {
    /**
     * 判空
     */
    fun isEmpty(str: String?): Boolean{
        str ?: return true
        return str.isEmpty()
    }
}