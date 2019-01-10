package com.joe.utils.proxy.bytebuddy;

import com.joe.utils.collection.CollectionUtil;
import com.joe.utils.common.StringUtils;
import com.joe.utils.proxy.Interception;
import com.joe.utils.proxy.ProxyClassLoader;
import com.joe.utils.proxy.ProxyClient;
import com.joe.utils.reflect.ClassUtils;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * 代理客户端bytebuddy实现
 *
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2018年11月07日 11:31 JoeKerouac Exp $
 */
@Slf4j
public class ByteBuddyProxyClient implements ProxyClient {
    private static final AnyMethodElementMatcher MATCHER = new AnyMethodElementMatcher();

    @Override
    public <T> T create(Class<T> parent, T proxy, ClassLoader loader, String name,
                        Interception interception) {
        DynamicType.Builder<T> builder = new ByteBuddy().subclass(parent);

        if (!StringUtils.isEmpty(name)) {
            builder = builder.name(name);
        }

        if (loader == null) {
            loader = ProxyClient.DEFAULT_LOADER;
        }

        builder = builder.method(MATCHER)
            .intercept(MethodDelegation.to(new GeneralInterceptor(interception, parent, proxy)));
        ProxyClassLoader realLoader;
        if (loader instanceof ProxyClassLoader) {
            realLoader = (ProxyClassLoader) loader;
        } else {
            realLoader = new ProxyClassLoader(loader);
        }
        Class<? extends T> clazz = builder.make()
            .load(realLoader,
                (classLoader, types) -> CollectionUtil.convert(types, classLoader::buildClass))
            .getLoaded();
        return ClassUtils.getInstance(clazz);
    }

    @Override
    public ClientType getClientType() {
        return ClientType.BYTE_BUDDY;
    }

    /**
     * 匹配任何方法
     */
    private static class AnyMethodElementMatcher implements ElementMatcher<MethodDescription> {

        @Override
        public boolean matches(MethodDescription target) {
            return true;
        }
    }
}
