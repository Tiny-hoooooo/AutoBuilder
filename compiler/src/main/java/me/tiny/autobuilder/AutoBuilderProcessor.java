package me.tiny.autobuilder;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import me.tiny.annotation.AutoBuilder;
import me.tiny.annotation.Ignore;
import me.tiny.autobuilder.exceptions.RuleRejectedException;
import me.tiny.autobuilder.rules.AbstractClassRejectRule;
import me.tiny.autobuilder.rules.ConstructorRejectRule;
import me.tiny.autobuilder.rules.Rule;

/**
 * Created by beichen on 16/7/24.
 *
 */
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
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(AutoBuilder.class)) {
            //判断当前Element是否是类,不用 annotatedElement instanceof TypeElement的原因是interface也是TypeElement.
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
        String packageName = processingEnv.getElementUtils().getPackageOf(annotatedClass).getQualifiedName().toString();
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
