package kim.nzxy.spel

import kim.nzxy.spel.yaml.YamlDocInfo

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

    val spELDocMap = mapOf(
        ".fields" to YamlDocInfo(
            "Map<String, String>", "java.util.Map<String, String>", "",
            "Custom SpEL variables mapping. For instance, `.fields.root=com.example.RootObject`"
        ),
        ".method.result" to YamlDocInfo(
            "Boolean", "Boolean", "false",
            "Support method result type, named that method.resultName"
        ),
        ".method.parameters" to YamlDocInfo(
            "Boolean", "Boolean", "false",
            "if method, support parameter list as SpEL variables"
        ),
        ".method.resultName" to YamlDocInfo(
            "String", "String", "result",
            "Variable name of method result"
        ),
        ".method.parametersPrefix" to YamlDocInfo(
            "String[]", "String[]", "[]",
            "if provided, parameter name will be prefixed"
        ),
    )
}