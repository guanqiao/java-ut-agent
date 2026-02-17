package com.utagent.di;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ServiceContainer {

    private final Map<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();
    private final Map<Class<?>, Boolean> isSingleton = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
        isSingleton.put(type, false);
    }

    public <T> void registerSingleton(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
        isSingleton.put(type, true);
    }

    public <T> void registerInstance(Class<T> type, T instance) {
        factories.put(type, () -> instance);
        singletons.put(type, instance);
        isSingleton.put(type, true);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        if (!factories.containsKey(type)) {
            throw new ServiceNotFoundException("Service not registered: " + type.getName());
        }

        if (isSingleton.getOrDefault(type, false)) {
            return (T) singletons.computeIfAbsent(type, t -> factories.get(t).get());
        }

        return (T) factories.get(type).get();
    }

    public boolean contains(Class<?> type) {
        return factories.containsKey(type);
    }

    public <T> void unregister(Class<T> type) {
        factories.remove(type);
        singletons.remove(type);
        isSingleton.remove(type);
    }

    public void clear() {
        factories.clear();
        singletons.clear();
        isSingleton.clear();
    }
}
