# SpEL Extension IDEA

## 简介

用于支持IDEA中自定义注解支持SpEL表达式自定义上下文, 方法参数/返回值, 以及自定义root节点

注意: 此插件仅仅是SpEL在IDEA中的代码提示, 并非写上注解就支持解析了, 得自己写解析流程

![demo](./doc/img/demo.png)

## 使用

1. 引入相关注解

   in `pom.xml`(Maven)

   ```xml
   <dependency>
       <groupId>kim.nzxy</groupId>
       <artifactId>spel-extension</artifactId>
       <version>1.0.0</version>
       <scope>provided</scope>
   </dependency>
   ```

   or `build.gradle`(Groovy DSL)

   ```groovy
   dependencies {
       compileOnly 'kim.nzxy:spel-extension:1.0.0'
   }
   ```

   or `build.gradle.kts`(Kotlin DSL)

   ```kotlin
   dependencies {
       compileOnly("kim.nzxy:spel-extension:1.0.0")
   }
   ```

2. [安装此插件](https://plugins.jetbrains.com/plugin/23337-spel-extension/)

## 说明

以下是各个注解的详细解释

### `@kim.nzxy.spel.SpELMethod`----函数相关

如果标注了SpELMethod的注解作用于方法上, 则提示方法的返回值和参数信息

| 名称             | 类型     | 默认值     | 解释                                                         |
| ---------------- | -------- | ---------- | ------------------------------------------------------------ |
| result           | boolean  | false      | 为true则支持方法返回值                                       |
| resultName       | String   | "result"   | 方法返回值的变量名称                                         |
| parameters       | boolean  | false      | 为true表示支持方法参数, 默认支持所有参数名称                 |
| parametersPrefix | String[] | {"p", "a"} | 方法参数序号名称, 如p0/a0表示第一个参数<br />如不需要, 可手动指定`parametersPrefix = {}` |

## `@kim.nzxy.spel.SpELField`--字段相关

定义该字段的变量参数, 支持定义多个

| 名称    | 类型     | 默认值       | 解释                                                         |
| ------- | -------- | ------------ | ------------------------------------------------------------ |
| name    | String   | \            | SpEL变量名, 如果为"root", 则视为SpEL的root节点               |
| type    | Class<?> | Object.class | 变量类型, 如果`typeStr`非空, 则忽略此字段                    |
| typeStr | String   | ""           | 变量类型, 支持泛型, 示例: `String`, `java.lang.String`, `java.util.List<String>` |

### `@kim.nzxy.spel.SpELWith`--方便定义常量

如果参数特别长, 可以用来方便的定义常量, 此注解可以方便的在常量字符串中支持代码提示

| 名称  | 类型                        | 默认值  | 解释             |
| ----- | --------------------------- | ------- | ---------------- |
| anno  | Class<? extends Annotation> | \       | 对应注解         |
| field | String                      | "value" | 对应注解字段名称 |
