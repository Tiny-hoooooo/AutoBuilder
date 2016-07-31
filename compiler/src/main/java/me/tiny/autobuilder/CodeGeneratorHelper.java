package me.tiny.autobuilder;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import me.tiny.annotation.Ignore;

/**
 * Created by beichen on 16/7/24.
 */
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
