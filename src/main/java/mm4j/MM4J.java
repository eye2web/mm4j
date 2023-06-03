package mm4j;

import mm4j.annotation.Constructor;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MM4J {
    private MM4J() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T createMapper(final Class<T> iFace) {
        return (T) Proxy.newProxyInstance(iFace.getClassLoader(), new Class[]{iFace}, new MM4JInvocationHandler());
    }

    private static class MM4JInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method method, final Object... args) throws Throwable {

            if (method.getGenericParameterTypes()[0] == method.getReturnType()) {
                return args[0];
            }

            final var returnClass = method.getReturnType();

            final java.lang.reflect.Constructor<?> constructor;
            final Object[] constructorArgs;

            final var inputMethodsOpt = parseConstructorAnnotation(method, args[0]);

            if (inputMethodsOpt.isPresent()) {
                final var inputMethods = inputMethodsOpt.get();
                constructor = findMatchingConstructor(returnClass.getConstructors(), inputMethods, true);
                constructorArgs = createConstructorArgs(constructor.getParameters(), args[0], inputMethods);
            } else {
                constructor = findMatchingConstructor(returnClass.getConstructors(), args[0]);
                constructorArgs = createConstructorArgs(constructor.getParameters(), args[0], getAvailableMethods(args[0]));
            }

            return constructor.newInstance(constructorArgs);
        }

        private Optional<List<Method>> parseConstructorAnnotation(final Method callerMethod, final Object inputObject) {
            final var constructorOpt = Arrays.stream(callerMethod.getAnnotations())
                    .filter(annotation -> annotation.annotationType().equals(Constructor.class))
                    .findFirst();

            if (constructorOpt.isPresent()) {
                final var constructorAnnotation = (Constructor) constructorOpt.get();

                final var methods = getAvailableMethods(inputObject);

                return Optional.of(Arrays.stream(constructorAnnotation.mappings())
                        .map(mapping ->
                                methods.stream().filter(m ->
                                                constructorAnnotation.caseSensitive() ?
                                                        m.getName().equals(mapping) :
                                                        m.getName().toLowerCase(Locale.ROOT).equals(mapping.toLowerCase(Locale.ROOT))
                                        )
                                        .findFirst()
                                        .orElseThrow(RuntimeException::new)
                        ).collect(Collectors.toList()));
            }
            return Optional.empty();
        }

        private Object[] createConstructorArgs(final Parameter[] constructorParams, final Object inputObject, final List<Method> methods) {
            return Arrays.stream(constructorParams)
                    .map(constructorParam -> {
                        final var method = findMatchingMethod(constructorParam, methods);
                        methods.remove(method);
                        try {
                            return method.invoke(inputObject);
                        } catch (final InvocationTargetException | IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .toArray(Object[]::new);

        }

        private java.lang.reflect.Constructor<?> findMatchingConstructor(final java.lang.reflect.Constructor<?>[] outputConstructors,
                                                                         final Object inputObject) {

            final var methods = getAvailableMethods(inputObject);
            return findMatchingConstructor(outputConstructors, methods, false);
        }

        private java.lang.reflect.Constructor<?> findMatchingConstructor(final java.lang.reflect.Constructor<?>[] outputConstructors,
                                                                         final List<Method> inputMethods, final boolean exactMatch) {
            final var methods = new ArrayList<>(inputMethods);
            // Method count should be equal or greater then constructor arguments needed
            // AND
            // All constructor argument types should exist as method return types
            return Arrays.stream(outputConstructors)
                    .filter(constructor -> exactMatch ? constructor.getParameterCount() == methods.size() :
                            constructor.getParameterCount() <= methods.size())
                    .filter(constructor ->
                            shouldMatchAllTypes(constructor, methods)
                    )
                    .findFirst().orElseThrow(RuntimeException::new);
        }

        private boolean shouldMatchAllTypes(final java.lang.reflect.Constructor<?> constructor, final List<Method> methods) {
            return Arrays.stream(constructor.getParameters())
                    .allMatch(param -> {
                        final var methodResult = methods.stream()
                                .filter(method -> method.getReturnType().equals(param.getType()))
                                .findFirst();

                        methodResult.ifPresent(methods::remove);

                        return methodResult.isPresent();
                    });
        }

        private Method findMatchingMethod(final Parameter param, final List<Method> methods) {
            return methods.stream()
                    .filter(method -> method.getReturnType().equals(param.getType()))
                    .filter(method -> filterByParamName(param, method))
                    .findFirst().orElseThrow(RuntimeException::new);
        }

        private boolean filterByParamName(final Parameter param, final Method method) {
            return !param.isNamePresent() || method.getName().equals(param.getName());
        }

        private List<Method> getAvailableMethods(final Object obj) {
            final var declareMethods = obj.getClass().getDeclaredMethods();
            // Need to remove the default added Java methods.
            return Arrays.stream(Arrays.copyOfRange(declareMethods, 3, declareMethods.length))
                    .collect(Collectors.toList());
        }

    }
}
