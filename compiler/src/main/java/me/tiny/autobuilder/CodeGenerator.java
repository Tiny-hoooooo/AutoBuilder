package me.tiny.autobuilder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * Created by beichen on 16/7/25.
 */
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
