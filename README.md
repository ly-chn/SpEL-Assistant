# SpEL Assistant

## 简介

用于支持IDEA中自定义注解支持SpEL表达式自定义上下文, 方法参数/返回值, 以及自定义root节点

注意: 此插件仅仅是SpEL在IDEA中的代码提示, 并非写上注解就支持解析了, 得自己写解析流程

![demo](./doc/img/demo.png)

### 使用

1. [安装此插件](https://plugins.jetbrains.com/plugin/23542-spel-assistant)

2. 在项目中resource文件夹下新建`spel-extension.json`配置文件

   > 如果您引入的第三方库也包含此文件, 则会一并纳入到配置中, 其中针对同一个注解上的字段的配置, 本地配置将会覆盖库中的配置

3. 正常在项目中调用即可, 如果没能及时生效, 请尝试手动保存一下`spel-extension.json`文件, 以便快速生效

### 配置详解

```json5
{
   // 注解声明, 格式为`注解类@字段`
   "kim.nzxy.demo.DemoAop@value": {
      // 模板前缀, 为null和空字符串表示非模板, 默认为空
      "prefix": "#{",
      // 模板后缀, 为null和空字符串表示非模板, 默认为空
      "suffix": "}",
      // 对方法的扩展, 默认值如示例
      "method": {
         // 作用于方法上时, 支持方法返回值提示
         "result": false,
         // 方法返回值的SpEL变量名
         "resultName": "result",
         // 作用于方法上时, 支持方法参数提示
         "parameters": false,
         // 方法参数别名配置, 支持多个别名前缀, 如 [p0, a1, p2]分表表示第一个, 第二个, 第三个参数, 可空 
         "parametersPrefix": [
            "p",
            "a"
         ]
      },
      // 自定义字段, 默认为空
      "fields": {
         // 自定义变量以及类型, 支持非限定类名, 如String, Integer等, 否则需提供全限定类名
         "demo": "java.util.Map<String, String>",
         // 如果变量名为root, 则表示rootObject(可以直接)
         "root": "kim.nzxy.demo.DemoRoot"
      }
   }
}
```