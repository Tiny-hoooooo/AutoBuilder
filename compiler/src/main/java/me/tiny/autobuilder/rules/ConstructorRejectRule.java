package me.tiny.autobuilder.rules;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import me.tiny.autobuilder.exceptions.ConstructorRejectedException;
import me.tiny.autobuilder.exceptions.RuleRejectedException;

/**
 * Created by beichen on 16/7/24.
 * 用于判断是否提供非private的无参构造函数
 */
public class ConstructorRejectRule implements Rule<TypeElement> {

    @Override
    public void validateRule(TypeElement element) throws RuleRejectedException {
        for (ExecutableElement executableElement : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            if (!executableElement.getModifiers().contains(Modifier.PRIVATE) && executableElement.getParameters().isEmpty()) {
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
