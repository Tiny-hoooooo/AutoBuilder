package me.tiny.autobuilder.rules;

import javax.lang.model.element.Element;

import me.tiny.autobuilder.exceptions.RuleRejectedException;

/**
 * Created by beichen on 16/7/24.
 * 规则校验接口
 */
public interface Rule<T extends Element> {

    void validateRule(T element) throws RuleRejectedException;

    RuleRejectedException throwException(T element);
}
