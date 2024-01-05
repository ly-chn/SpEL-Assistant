package kim.nzxy.spel


@Suppress("ConstPropertyName")
object SpELConst {
    const val objClass = "java.lang.Object"
    const val rootName = "root"
    const val defaultAttrName = "value"

    const val methodAnno = "kim.nzxy.spel.SpELMethod"
    const val methodAttrResult = "result"
    const val methodAttrResultDefault = false
    const val methodAttrResultName = "resultName"
    const val methodAttrResultNameDefault = "result"
    const val methodAttrParams = "parameters"
    const val methodAttrParamsDefault = false
    const val methodAttrParamsPrefix = "parametersPrefix"
    val methodAttrParamsPrefixDefault = arrayOf("p", "a")

    const val fieldAnno = "kim.nzxy.spel.SpELField"
    const val fieldAttrName = "name"
    const val fieldAttrType = "type"
    const val fieldAttrTypeDefault = objClass
    const val fieldAttrTypeStr = "typeStr"
    const val fieldAttrTypeStrDefault = ""

    const val spELWithAnno = "kim.nzxy.spel.SpELWith"
    const val spELWithAttrAnno = "anno"
    const val spELWithAttrField = "field"
    const val spELWithAttrFieldDefault = "value"

    val spELInjectTarget = arrayOf(methodAnno, fieldAnno, spELWithAnno)
}