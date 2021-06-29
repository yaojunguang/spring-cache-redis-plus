package com.smarthito.cache.cache;


import org.springframework.aop.framework.AopProxyUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 标记了缓存注解的方法类信息
 * 用于主动刷新缓存时调用原始方法加载数据
 *
 * @author yuhao.wang
 */
public class CachedMethodInvocation implements Serializable {

    private static final long serialVersionUID = 4053574712960451939L;

    private Object key;
    private String targetBean;
    private String targetMethod;
    private List<Object> arguments;
    private List<String> parameterTypes = new ArrayList<>();

    public CachedMethodInvocation() {
    }

    public CachedMethodInvocation(Object key, Object targetBean, Method targetMethod, Class[] parameterTypes, Object[] arguments) {
        this.key = key;
        this.targetBean = targetBean.getClass().getName();
        if (this.targetBean.startsWith("com.sun.proxy")) {
            Class<?>[] interfaces = targetBean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                this.targetBean = interfaces[0].getName();
            }
        }
        //
        //this.targetBean = targetClass.getName();
        this.targetMethod = targetMethod.getName();
        if (arguments != null && arguments.length != 0) {
            this.arguments = Arrays.asList(arguments);
        }
        if (parameterTypes != null && parameterTypes.length != 0) {
            for (Class clazz : parameterTypes) {
                this.parameterTypes.add(clazz.getName());
            }
        }
    }

    private Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null && target != null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public String getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(String targetBean) {
        this.targetBean = targetBean;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CachedMethodInvocation that = (CachedMethodInvocation) o;

        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
