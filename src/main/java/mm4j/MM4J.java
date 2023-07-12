package mm4j;

import mm4j.annotation.Constructor;
import mm4j.annotation.Mapping;
import mm4j.annotation.Mappings;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MM4J {
    private MM4J() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T createMapper(final Class<T> iFace) {
        return (T) Proxy.newProxyInstance(iFace.getClassLoader(), new Class[]{iFace}, new MM4JInvocationHandler());
    }

    private static class MM4JInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(final Object proxy, final Method callerMethod, final Object... args) throws Throwable {

            final var inputObj = args[0];

            if (callerMethod.getGenericParameterTypes()[0] == callerMethod.getReturnType()) {
                return inputObj;
            }

            final var returnClass = callerMethod.getReturnType();

            final java.lang.reflect.Constructor<?> constructor;
            final Object[] constructorArgs;

            final var inputMethodsOpt = parseConstructorAnnotation(callerMethod, inputObj);

            final var mappingAnnotations = getMappingAnnotations(callerMethod);

            if (inputMethodsOpt.isPresent()) {
                final var inputMethods = inputMethodsOpt.get();
                constructor = findMatchingConstructor(returnClass.getConstructors(), inputMethods, true);
                constructorArgs = createConstructorArgs(constructor.getParameters(), inputObj, inputMethods, mappingAnnotations);
            } else {
                constructor = findMatchingConstructor(returnClass.getConstructors(), inputObj);
                constructorArgs = createConstructorArgs(constructor.getParameters(), inputObj, getAvailableMethods(inputObj), mappingAnnotations);
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
                                                        m.getName().equalsIgnoreCase(mapping)
                                        )
                                        .findFirst()
                                        .orElseThrow(RuntimeException::new)
                        ).collect(Collectors.toList()));
            }
            return Optional.empty();
        }

        private Object[] createConstructorArgs(final Parameter[] constructorParams, final Object inputObject,
                                               final List<Method> methods, final List<Mapping> mappingAnnotations) {

            return Arrays.stream(constructorParams)
                    .map(constructorParam -> {

                        final var method = getMatchingMappingAnnotation(mappingAnnotations, constructorParam)
                                .map(mapping ->
                                        findMatchingMethod(constructorParam, methods, mapping.mapFrom(), mapping.caseSensitive())
                                ).orElseGet(() ->
                                        findMatchingMethod(constructorParam, methods)
                                );

                        methods.remove(method);
                        try {
                            return method.invoke(inputObject);
                        } catch (final InvocationTargetException | IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .toArray(Object[]::new);

        }

        private Optional<Mapping> getMatchingMappingAnnotation(final List<Mapping> mappingAnnotations, final Parameter param) {
            if (!param.isNamePresent()) {
                return Optional.empty();
            }

            return mappingAnnotations.stream()
                    .filter(mapping ->
                            mapping.caseSensitive() ?
                                    mapping.mapTo().equals(param.getName()) :
                                    mapping.mapTo().equalsIgnoreCase(param.getName())

                    ).findFirst();
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

        private Method findMatchingMethod(final Parameter param, final List<Method> methods, final String mapFrom,
                                          final boolean isCaseSensitive) {
            return methods.stream()
                    .filter(method -> method.getReturnType().equals(param.getType()))
                    .filter(method -> isCaseSensitive ? method.getName().equals(mapFrom) : method.getName().equalsIgnoreCase(mapFrom))
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

        private List<Mapping> getMappingAnnotations(final Method callerMethod) {
            return Arrays.stream(callerMethod.getAnnotations())
                    .filter(annotation -> annotation.annotationType().equals(Mappings.class))
                    .map(mappings ->
                            ((Mappings) mappings).value()
                    ).flatMap(Stream::of)
                    .collect(Collectors.toList());
        }
    }
}
