package me.tiny.autobuilder.rules;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import me.tiny.annotation.AutoBuilder;
import me.tiny.autobuilder.exceptions.AbstractClassRejectedException;
import me.tiny.autobuilder.exceptions.RuleRejectedException;

/**
 * Created by beichen on 16/7/24.
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
