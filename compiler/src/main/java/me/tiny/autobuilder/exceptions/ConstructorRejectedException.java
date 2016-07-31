package me.tiny.autobuilder.exceptions;

/**
 * Created by beichen on 16/7/24.
 */
public class ConstructorRejectedException extends RuleRejectedException {

    public ConstructorRejectedException(String errMsg) {
        super(errMsg);
    }

}
