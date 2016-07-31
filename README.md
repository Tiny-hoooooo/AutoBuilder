# 自己动手写注解框架

## 前言

刚接触Java的时候就觉得注解是非常神奇，加之现在越来越多的开源项目采用注解的方式来实现，如Dagger2，ButterKnife。因此在空余时间好好研究了一下，本文将向你介绍一些自定义注解所需要的基础知识以及一个简单的例子。

## 基础知识

### 元注解

所谓的元注解就是注解的注解。Java提供了4个元注解，分别是：

1. `@Target`：用于描述注解的使用范围，如果自定义注解不存在`@Target`，则表示该注解可以使用在任何程序元素之上。接收参数ElementType，其值如下：

   ```	java
   /**接口、类、枚举、注解**/
   ElementType.TYPE				  
   /**字段、枚举的常量**/
   ElementType.FIELD
   /**方法**/
   ElementType.METHOD				 
   /**方法参数**/
   ElementType.PARAMETER 			  
   /**构造方法**/
   ElementType.CONSTRUCTOR  		  
   /**局部变量**/
   ElementType.LOCAL_VARIABLE 		  
   /**注解**/
   ElementType.ANNOTATION_TYPE		  
   /**包**/
   ElementType.PACKAGE				  
   /**表示该注解能写在类型变量的声明语句中。 java8新增**/
   ElementType.TYPE_PARAMETER 			
   /**表示该注解能写在使用类型的任何语句中。 java8新增**/
   ElementType.TYPE_USE				
   ```

2. `@Retention`：表示注解类型保留的时长，它接收RetentonPolicy参数，其值如下：

   ```java
   /**注解仅存在于源码中，在编译阶段丢弃。这些注解在编译结束之后就不再有任何意义，所以它们不会写入字节码。**/
   RetentionPolicy.SOURCE  
   /**默认的保留策略，注解会在class字节码文件中存在，但运行时无法获得。**/
   RetentionPolicy.CLASS   
   /**注解会在class字节码文件中存在，在运行时可以通过反射获取到。**/
   RetemtionPolicy.RUNTIME 
   ```

3. `@Documented`: 表示注解可以出现在javadoc中。

4. `@Inherited`：表示注解可以被子类继承。

### Annotation Processor Tool

Annotation Processor Tool是用于编译期扫描和处理注解的工具，目前被集成在javac中。在编译的时候，javac通常会找到你定义的注解处理器，并执行注解处理。

不过遗憾的是，Android Studio默认是不支持注解处理器的，我们需要引入一个额外的Gradle插件，`android-apt`，这个插件功能是：允许配置只在编译时作为注解处理器的依赖，而不添加到最后的APK或library；设置源路径，使注解处理器生成的代码能被Android Studio正确的引用。

### AbstractProcessor

AbstractProcessor 是 javac 扫描和处理注解的关键类,所有自定义的Processor都是继承自AbastractProcessor,一个基本的Procssor结构如下所示：

```java
public class SimpleProcessor extends AbstractProcessor {
  
	/**
	 * 每一个注解处理器类都必须有一个无参构造方法。
	 * init方法是在Processor创建时被javac调用并执行初始化操作。
	 * @param processingEnv 提供一系列的注解处理工具。
	 **/
    @Override
    public synchronized void init(ProcessingEnvironment env){ }

    /**
     * 注解处理需要执行一次或者多次。每次执行时，处理器方法被调用，并且传入了当前要处理的注解类型。
     * 可以在这个方法中扫描和处理注解，并生成Java代码。
     * @param annotations 当前要处理的注解类型
     * @param roundEnv 这个对象提供当前或者上一次注解处理中被注解标注的源文件元素。（获得所有被标注的元   素）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env) { }

  	/** 注解处理器要处理的注解类型,值为完全限定名（就是带所在包名和路径的类全名） **/
    @Override
    public Set<String> getSupportedAnnotationTypes() { }
	
  	/** 指定支持的 java 版本，通常返回 SourceVersion.latestSupported() **/
    @Override
    public SourceVersion getSupportedSourceVersion() { }

}
```

有一点需要注意，Android Library中去除了javax包的部分功能，所以，在新建Module的时候不能选Android Library，需要使用Java Library。

### 注册Processor

想要让javac执行期间调用我们自定义的Processor，我们需要注册自定义的Processor:
方法一：在main文件夹下创建**resources/META-INF/javax.annotation.processing.Processor**，在该文件中的内容是以换行符分隔的Processor的完成限定类名（带包名的）：

```java
me.tiny.autobuilder.AutoBuilderProcessor
me.tiny.other.OtherProcessor
```

方法二：使用Google提供的@AutoSerivce注解：
引入依赖：

```groovy
dependencies {
    compile 'com.google.auto.service:auto-service:1.0-rc2'
}
```

使用@AutoService生成META-INF/services/javax.annotation.processing.Processor文件:

```java
AutoService(Processor.class)
public class AutoBuilderProcessor extends AbstractProcessor {
	...
}
```

## 实战：AutoBuilder

ok，理论知识差不多介绍完毕，下面让我们直接进入实战环节。

在这里我简单介绍一下AutoBuilder这个项目的结构，该项目主要分为两个Module，一个是library，另一个是compiler。library主要是放置所有自定义的注解类。而compiler则用于处理注解、生成相应代码。

### library module

附上library工程目录结构：

    library
    └── src
        └── main
            └── java
                └── me
                    └── tiny
                        └── annotation
                            ├── AutoBuilder.java
                            └── Ignore.java
可以看到library非常简单，只定义了两个注解：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoBuilder {
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Ignore {
}
```

### compiler module

附上compiler工程目录结构：

```
compiler
├── build
│   ├── classes
│      └── main
│          ├── META-INF
│            └── services
│                └── javax.annotation.processing.Processor
└── src
    └── main
        └── java
            └── me
                └── tiny
                    └── autobuilder
                        ├── AutoBuilderProcessor.java
                        ├── CodeGenerator.java
                        ├── CodeGeneratorHelper.java
                        ├── exceptions
                        │   ├── AbstractClassRejectedException.java
                        │   ├── ConstructorRejectedException.java
                        │   └── RuleRejectedException.java
                        └── rules
                            ├── AbstractClassRejectRule.java
                            ├── ConstructorRejectRule.java
                            └── Rule.java
```

在创建complier module时需要注意使用**Java library**，并且在项目顶层的build.gradle文件中添加`android-apt`插件依赖，具体代码如下：

```groovy
buildscript {
	    repositories {
	        jcenter()
	    }
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.1.0'
	        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
	    }
	}
```

然后我们在compiler的builder.gradle中添加library和google auto service的依赖：

```groovy
dependencies {
    ...
	compile 'com.google.auto.service:auto-service:1.0-rc2'
	compile project(":library")
}
```

#### AutoBuilderProcessor

之前说过，自定义的Processor是javac扫描和处理注解的关键类，让我们来看一下我们的处理器类：

```java
@AutoService(Processor.class)
public class AutoBuilderProcessor extends AbstractProcessor {

    private List<Rule> mRules;

    private Messager mErrorMessager;

    /**
     * 每一个注解处理器类都必须有一个无参构造方法。
     * init方法是在Processor创建时被apt调用并执行初始化操作。
     * @param processingEnv 提供一系列的注解处理工具。
     **/
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mRules = new ArrayList<>();
        mRules.add(new AbstractClassRejectRule());
        mRules.add(new ConstructorRejectRule());
        mErrorMessager = processingEnv.getMessager();
    }

    /**
     * 注解处理需要执行一次或者多次。每次执行时，处理器方法被调用，并且传入了当前要处理的注解类型。
     * 可以在这个方法中扫描和处理注解，并生成Java代码。
     * @param annotations 当前要处理的注解类型
     * @param roundEnv  这个对象提供当前或者上一次注解处理中被注解标注的源文件元素。（获得所有被标注的元素）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
   
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(AutoBuilder.class)) {
			//判断当前Element是否是类。
          	//不用 annotatedElement instanceof TypeElement的原因是interface也是TypeElement.
            if (annotatedElement.getKind() == ElementKind.CLASS) {
                TypeElement annotatedClass = (TypeElement) annotatedElement;
                try {
                    validateRule(annotatedClass);
                } catch (RuleRejectedException e) {
                    mErrorMessager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    return true;
                }
                generateCode(annotatedClass);
            }
        }
        return true;
    }

    private void generateCode(TypeElement annotatedClass) {
        //获取包名
        String packageName = processingEnv.getElementUtils()
          .getPackageOf(annotatedClass).getQualifiedName().toString();
        CodeGenerator codeGenerator = new CodeGenerator(packageName, annotatedClass);

        try {
            codeGenerator.generateJavaFile(processingEnv.getFiler());
        } catch (IOException e) {
            mErrorMessager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    /**
     * @return 返回支持的Annotation类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new HashSet<>();
        supportedAnnotationTypes.add(Ignore.class.getCanonicalName());
        supportedAnnotationTypes.add(AutoBuilder.class.getCanonicalName());
        return supportedAnnotationTypes;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @SuppressWarnings("unchecked")
    private void validateRule(TypeElement element) throws RuleRejectedException {
        for (Rule rule : mRules) {
            rule.validateRule(element);
        }
    }
}
```

##### init

在init方法中我们通过super.init(processingEnv)方法得到了processingEnv的引用。通过processingEnv对象我们能获得如下引用：

- Elements:一个处理Element的的工具类。
- Types：一个处理TypeMirror的工具类。
- Filer：定义了一些关于创建源文件，类文件和一般资源的方法。
- Messager：提供给注解处理器一个报告错误、警告以及提示信息的途径，它不是注解处理器开发者的日志工具，而是用来写一些信息给使用此注解器的第三方开发者的。

##### process

首先，需要说明一下Element的含义，Element代表程序的元素，例如包、类、方法、成员变量。对应关系如下：

```
PackageElement   		--->	包
ExecuteableElement		--->	方法、构造方法
VariableElement			--->	成员变量、enum常量、方法或构造方法参数、局部变量或异常参数。
TypeElement				--->	类、接口
TypeParameterElement	--->	在方法或构造方法、类、接口处定义的泛型参数。
```

在process中我们通过RoundEnvironment对象的getElementsAnnotatedWith方法获得所有包含@AutoBuilder注解的Element的集合。接下来，我们必须检查这些Element是否是一个类：

```java
...
//判断当前Element是否是类。
//不用 annotatedElement instanceof TypeElement的原因是interface也是TypeElement.
if (annotatedElement.getKind() == ElementKind.CLASS) {
  ...
}
...
```

然后，我们需要校验该类是否满足生成Builder类的规则，规则如下：

```java
/**
 * 校验是否是抽象方法
 */
public class AbstractClassRejectRule implements Rule<TypeElement> {

    @Override
    public void validateRule(TypeElement element) throws RuleRejectedException {
        if (element.getModifiers().contains(Modifier.ABSTRACT)) {
            throw throwException(element);
        }
    }

    @Override
    public RuleRejectedException throwException(TypeElement element) {

        return new AbstractClassRejectedException(
                String.format("The class %s is abstract. You can't annotate abstract classes with @%s",
                        element.getQualifiedName().toString(), AutoBuilder.class.getSimpleName()));
    }
}

/**
 * 用于判断是否提供非private的无参构造函数
 */
public class ConstructorRejectRule implements Rule<TypeElement> {

    @Override
    public void validateRule(TypeElement element) throws RuleRejectedException {
        for (ExecutableElement executableElement : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            if (!executableElement.getModifiers().contains(Modifier.PRIVATE) 
                && executableElement.getParameters().isEmpty()) {
                return;
            }
        }
        throw throwException(element);
    }

    @Override
    public RuleRejectedException throwException(TypeElement element) {
        return new ConstructorRejectedException(
                String.format("The class %s must provide an non-private empty constructor", element.getQualifiedName().toString()));
    }
}
```

如果该TypeElement不满足规则，会抛出一个错误，我们需要在process中捕获错误并通过processingEnv提供的Messager类将错误信息发送给第三方开发者，方便他们找到错误原因。

还有一点需要注意的是process方法可能会被多次执行，当我们生成新的源文件时，它就会被再次执行（只有一次init，process会执行多次）。如果重新创建已经生成的源代码，将会抛出一个异常。



##### 代码生成

使用Square公司出品的[JavaPoet](https://github.com/square/javapoet)来生成java源代码。

```java
public class CodeGenerator {

    private final String mPackageName;

    private final AnnotatedClass mAnnotatedClass;

    private final ClassName mAnnotatedClassName;

    private final ClassName mGeneratedClassName;

    private final static String SUFFIX = "Builder";

    public CodeGenerator(String packageName, TypeElement typeElement) {
        mPackageName = packageName;
        mAnnotatedClass = new AnnotatedClass(typeElement);
        mAnnotatedClassName = ClassName.get(packageName, typeElement.getSimpleName().toString());
        mGeneratedClassName = ClassName.get(packageName, typeElement.getSimpleName().toString() + SUFFIX);
    }

    private TypeSpec generateCode() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(mGeneratedClassName)
                //添加修饰符
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(builder())
                .addMethod(build());

        for (Element field : mAnnotatedClass.getFields()) {
            TypeName fieldClass = ClassName.get(field.asType());
            String fieldName = field.getSimpleName().toString();
            builder.addField(fieldClass, fieldName, Modifier.PRIVATE);
            builder.addMethod(MethodSpec.methodBuilder(fieldName)
                    .addParameter(fieldClass, fieldName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(mGeneratedClassName)
                    .addStatement("this.$L = $L", fieldName, fieldName)
                    .addStatement("return this")
                    .build());
        }
        return builder.build();
    }

    public void generateJavaFile(Filer filer) throws IOException {
        JavaFile javaFile = JavaFile.builder(mPackageName, generateCode()).build();
        javaFile.writeTo(filer);
    }

    /**
     * 创建build方法
     * @return MethodSpec
     */
    private MethodSpec build() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(mAnnotatedClassName)
                .addStatement("$T var = new $T()", mAnnotatedClassName, mAnnotatedClassName);

        for (Element field : mAnnotatedClass.getFields()) {
            String fieldName = field.getSimpleName().toString();
            builder.addStatement("var.$L = $L", fieldName, fieldName);
        }
        return builder.addStatement("return var").build();
    }

    /**
     * 创建builder方法
     * @return MethodSpec
     */
    private MethodSpec builder() {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(mGeneratedClassName)
                .addStatement("return new $T()", mGeneratedClassName)
                .build();
    }

    private static class AnnotatedClass {

        private List<Element> mFields;

        public AnnotatedClass(TypeElement typeElement) {
            mFields = CodeGeneratorHelper.filterFields(typeElement);
        }

        public List<Element> getFields() {
            return mFields;
        }
    }
}
```

```java
public class CodeGeneratorHelper {

    /**
     * @param element TypeElement
     * @return 过滤被@Ignore、static、final、private标识的字段
     */
    public static List<Element> filterFields(TypeElement element) {
        List<Element> elements = new ArrayList<>();
        for (Element builderField : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            boolean isIgnored = builderField.getAnnotation(Ignore.class) != null
                    || builderField.getModifiers().contains(Modifier.STATIC)
                    || builderField.getModifiers().contains(Modifier.FINAL)
                    || builderField.getModifiers().contains(Modifier.PRIVATE);
            if (!isIgnored) {
                elements.add(builderField);
            }
        }
        return elements;
    }
} 
```

### 使用AutoBuilder

想要使用AutoBuilder，我们需要在app的build.gradle中添加如下代码(使用apply plugin: 'com.neenbedankt.android-apt'的前提是已经在项目顶层的build.gradle中添加的android-apt的依赖):

```groovy
apply plugin: 'com.android.application'
apply plugin: 'com.neenbedankt.android-apt'
...
dependencies {
	...
    compile project(":library")
    apt project(':compiler')
}
```

```java
@AutoBuilder
public class Person {
	int age;
	String name;
	/**省略get、set方法**/
}

//生成的代码如下：

public final class PersonBuilder {
  private String name;
  private int age;
  
  public static PersonBuilder builder() {
    return new PersonBuilder();
  }

  public Person build() {
    Person var = new Person();
    var.name = name;
    var.age = age;
    var.address = address;
    return var;
  }

  public PersonBuilder name(String name) {
    this.name = name;
    return this;
  }

  public PersonBuilder age(int age) {
    this.age = age;
    return this;
  }
}

```

## 结束语

好了，这里抛砖引玉，简单介绍了一下注解的处理流程，更进一步的应用大家可以查看其他注解框架的源码。

第一次写博客，加之本人水平有限，有什么不对的地方还请大家指正。

---

## License

Copyright 2016 Tiny-Hoooooo

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
