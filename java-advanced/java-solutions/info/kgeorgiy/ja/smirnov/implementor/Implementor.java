package info.kgeorgiy.ja.smirnov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for generating classes, that implements/extends specified interfaces/classes.
 *
 * @author Andrew Smirnov
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 */
public class Implementor implements Impler, JarImpler {

    /**
     * ".java" string constant
     */
    private static final String JAVA = ".java";

    /**
     * "class" string constant
     */
    private static final String CLASS = "class";

    /**
     * "Impl" string constant
     */
    private static final String IMPL = "Impl";

    /**
     * {@link File#separatorChar} string constant
     */
    private static final char FILE_SEPARATOR = File.separatorChar;

    /**
     * " " string constant
     */
    private static final String SPACE = " ";

    /**
     * empty string constant
     */
    private static final String EMPTY_STRING = "";

    /**
     * "package" string constant
     */
    private static final String PACKAGE = "package";

    /**
     * ";" string constant
     */
    private static final String SEMICOLON = ";";

    /**
     * "public" string constant
     */
    private static final String PUBLIC = "public";

    /**
     * "extends" string constant
     */
    private static final String EXTENDS = "extends";

    /**
     * "implements" string constant
     */
    private static final String IMPLEMENTS = "implements";

    /**
     * "{" string constant
     */
    private static final String CURLY_OPEN_BRACKET = "{";

    /**
     * "}" string constant
     */
    private static final String CURLY_CLOSE_BRACKET = "}";

    /**
     * "(" string constant
     */
    private static final String ROUND_OPEN_BRACKET = "(";

    /**
     * ")" string constant
     */
    private static final String ROUND_CLOSE_BRACKET = ")";

    /**
     * "throws" string constant
     */
    private static final String THROWS = "throws";

    /**
     * "super" string constant
     */
    private static final String SUPER = "super";

    /**
     * "return" string constant
     */
    private static final String RETURN = "return";

    /**
     * {@link System#lineSeparator()} string constant
     */
    private static final String NEW_LINE = System.lineSeparator();

    /**
     * Method for generating name of class with Impl suffix
     *
     * @param token input class token
     * @return {@code token} name with suffix Impl
     */
    private String getClassImplName(Class<?> token) {
        return token.getSimpleName() + IMPL;
    }

    /**
     * Method for generating {@code Path} for class token without root
     *
     * @param token input class token
     * @return {@code Path} for provided {@code token}
     */
    private Path getTokenPath(Class<?> token) {
        return Path.of(token.getPackageName().replace('.', FILE_SEPARATOR))
                .resolve(getClassImplName(token) + JAVA);
    }

    /**
     * Method for creating parent directories of provided {@code path}
     *
     * @param path provided {@code Path}
     * @return provided {@code path}
     * @throws ImplerException if {@link Files#createDirectories(Path, FileAttribute[])}
     *                         throws {@code IOException}
     */
    private Path createDirectories(final Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Exception while creating parent directory");
            }
        }

        return path;
    }

    /**
     * Method for generating full {@code Path} of provided {@code token} and {@code root}
     *
     * @param token input class token
     * @param root  input root directory, where generated class should be placed
     * @return absolute {@code Path} of token
     * @throws ImplerException if {@link #createDirectories(Path)} throws exception
     */
    private Path getClassFile(Class<?> token, Path root) throws ImplerException {
        final Path result = root.resolve(getTokenPath(token));
        return createDirectories(result);
    }

    /**
     * Method for input {@code token} validation.
     * <p>
     * Token is valid if it's not array, enum, primitive type, {@code final} or {@code private} class
     *
     * @param token input class token
     * @return {@code true} if token is valid
     */
    boolean isValid(Class<?> token) {
        int tokenModifiers = token.getModifiers();

        return !token.isArray() &&
                token != Enum.class &&
                !token.isPrimitive() &&
                !Modifier.isFinal(tokenModifiers) &&
                !Modifier.isPrivate(tokenModifiers);
    }

    /**
     * Implementation of {@link Impler#implement(Class, Path)}.
     * <p>
     * Invokes {@link #generateClass(BufferedWriter, Class)} of provided
     * {@code token} and {@code writer} with {@code token} and {@code root}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if this {@code classPath} of {@code token} and {@code root} is not supported
     *                         or provided {@code token} is not valid
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (isValid(token)) {
            final Path classPath = getClassFile(token, root);

            try (final BufferedWriter writer = Files.newBufferedWriter(classPath)) {
                generateClass(writer, token);
            } catch (IOException e) {
                throw new ImplerException("Unsupported classpath exception, classpath = " + classPath);
            }
        } else {
            throw new ImplerException("Token is not supported, token = " + token.getCanonicalName());
        }

    }

    /**
     * Method for generating code for class which extends provided {@code token}.
     * <p>
     * Invokes {@link #generatePackage(Class)} {@link #generateClassHeader(Class)}
     * {@link #generateConstructors(Class)} {@link #generateMethods(Class)}
     *
     * @param writer which is used for writing class
     * @param token  super class or interface of future generated class
     * @throws ImplerException if {@link IOException} occurres while writing from {@code writer}
     */
    private void generateClass(BufferedWriter writer, Class<?> token) throws ImplerException {
        try {
            writer.write(String.join(NEW_LINE,
                    generatePackage(token),
                    generateClassHeader(token),
                    generateConstructors(token),
                    generateMethods(token),
                    CURLY_CLOSE_BRACKET
            ));
        } catch (IOException e) {
            throw new ImplerException("Exception while writing to file " + e.getMessage());
        }
    }

    /**
     * Method for generating note about package of class that extends provided {@code token}.
     * <p>
     * The result will be string "package <i>package of token;</i>"
     * or empty string if there is no information about package in {@code token}
     *
     * @param token super class or interface for future generated class
     * @return generated code with information about the package
     */
    private String generatePackage(Class<?> token) {
        String classPackage = token.getPackageName();

        return classPackage.isEmpty() ? EMPTY_STRING : String.join(SPACE, PACKAGE, classPackage, SEMICOLON);
    }

    /**
     * Method for generating code for class header of future class
     * <p>
     * The result will be string "public class <i>tokenImpl</i> implements/extends {@code token}"
     * depending on {@code token} is interface or class
     *
     * @param token super class or interface for future generated class
     * @return generated code for class header
     */
    private String generateClassHeader(Class<?> token) {
        return String.join(SPACE, PUBLIC, CLASS, getClassImplName(token),
                token.isInterface() ? IMPLEMENTS : EXTENDS, token.getCanonicalName(), CURLY_OPEN_BRACKET);
    }

    /**
     * Method for generating all non-private constructors
     * <p>
     * Generates non-private constructors
     * with {@link Class#getDeclaredConstructors()} and {@link #generateConstructor(Constructor, Class)},
     * separating them with new line. If {@code token} is interface, than returns empty string
     *
     * @param token super class or interface for future generated class
     * @return code for all non-private constructors of class
     * @throws ImplerException if {@code token} is class and there is not {@code public} constructors in it
     */
    private String generateConstructors(Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return EMPTY_STRING;
        }

        String result = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .map(constructor -> generateConstructor(constructor, token))
                .collect(Collectors.joining(NEW_LINE));

        if (result.isEmpty()) {
            throw new ImplerException("There is no one public constructor");
        }
        return result;
    }

    /**
     * Method for generating code for constructor
     * <p>
     * Generates string "public <i>tokenImpl</i>(<i>params</i>) {super(params);}"
     * with {@link #generateExecutableHeader(Executable, String)}
     * and {@link #generateArguments(Executable, boolean)}
     *
     * @param constructor provided constructor of the super class
     * @param token       super class
     * @return generated code for provided {@code constructor} and {@code token}
     */
    private String generateConstructor(Constructor<?> constructor, Class<?> token) {
        return String.join(NEW_LINE, generateExecutableHeader(constructor, getClassImplName(token)),
                String.join(EMPTY_STRING, SUPER, ROUND_OPEN_BRACKET,
                        generateArguments(constructor, false), ROUND_CLOSE_BRACKET, SEMICOLON),
                CURLY_CLOSE_BRACKET);
    }

    /**
     * Method for generating headers for methods/constructors
     * <p>
     * Generates string
     * "public {@code name} (<i>params with types</i>) throws <i>Exception1, Exception2, ...</i>"
     * with {@link #generateArguments(Executable, boolean)} and {@link #generateExceptions(Executable)}
     *
     * @param executable method or constructor
     * @param name       string with return type and name of method for methods (e.g. "int main")
     *                   or name of constructor for constructors (e.g. "Constructor")
     * @return generated code for header of provided {@code executable}
     */
    private String generateExecutableHeader(Executable executable, String name) {
        return String.join(SPACE, PUBLIC, name,
                ROUND_OPEN_BRACKET, generateArguments(executable, true), ROUND_CLOSE_BRACKET,
                generateExceptions(executable), CURLY_OPEN_BRACKET);
    }

    /**
     * Method for generating arguments for class or method
     * <p>
     * Generates string "<i>type1 name_of_arg1, type2 name_of_arg2 ...</i>"
     * if withTypes is true (used in {@link #generateExecutableHeader(Executable, String)})
     * and "<i>name_of_arg1, name_of_arg2 ...</i>" if it's false
     * (used in {@link #generateConstructor(Constructor, Class)} )
     *
     * @param executable class or method
     * @param withTypes  true if it's needed types before name of arguments: "int a", false: "a"
     * @return generated code of arguments for provided {@code executable}
     */
    private String generateArguments(Executable executable, boolean withTypes) {
        return Arrays.stream(executable.getParameters())
                .map(parameter -> String.join(SPACE, withTypes ?
                        parameter.getType().getCanonicalName() : EMPTY_STRING, parameter.getName()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Method for generating exceptions for constructors or methods
     * <p>
     * Obtains exceptions with {@link Executable#getExceptionTypes()}.
     * Generate string "throws Exception1, Exception2, ..."
     * or empty string if array with exceptions is empty
     *
     * @param executable constructor or method
     * @return code with information about exceptions to provided {@code executable}
     */
    private String generateExceptions(Executable executable) {
        var exceptions = executable.getExceptionTypes();

        return exceptions.length == 0 ? EMPTY_STRING :
                String.join(SPACE, THROWS, Arrays.stream(exceptions).map(Class::getCanonicalName).
                        collect(Collectors.joining(", ")));
    }

    /**
     * Method for generating methods of the class
     * <p>
     * Finds all abstract methods in inheritance tree with {@link #findMethodsThroughTree(Class)},
     * than filter all non-final methods in parent ({@code token}), than generates methods with
     * {@link #generateMethod(Method)}
     *
     * @param token super class or interface
     * @return all methods that {@code extends abstract} methods of provided {@code token}
     */
    private String generateMethods(Class<?> token) {
        return findMethodsThroughTree(token).entrySet().stream()
                .filter(entry -> !Modifier.isFinal(entry.getKey().getMethod().getModifiers()))
                .map(entry -> generateMethod(entry.getValue())).collect(Collectors.joining(NEW_LINE));
    }

    /**
     * Method for generating code for method
     * <p>
     * Generates code for method with {@link #generateExecutableHeader(Executable, String)}
     * in body will be string "return <i>default_value</i>"
     * which obtained by {@link #getDefaultType(Class)}
     *
     * @param method method for what we should generate code
     * @return generated code for provided {@code method}
     */
    private String generateMethod(Method method) {
        return String.join(NEW_LINE, generateExecutableHeader(method,
                        String.join(SPACE, method.getReturnType().getCanonicalName(), method.getName())),
                String.join(SPACE, RETURN, getDefaultType(method.getReturnType()), SEMICOLON), CURLY_CLOSE_BRACKET);
    }

    /**
     * Method for generating default value of {@code type}
     * <p>
     * Generates <ul>
     * <li>"null" for {@code Object}</li>
     * <li>"true" for {@code boolean}</li>
     * <li>empty string for {@code void}</li>
     * <li>"0" for primitive types</li>
     * </ul>
     *
     * @param type type for generating default value
     * @return default value of provided {@code type}
     */
    private String getDefaultType(Class<?> type) {
        if (!type.isPrimitive()) {
            return "null";
        }
        if (type.equals(boolean.class)) {
            return "true";
        }
        if (type.equals(void.class)) {
            return EMPTY_STRING;
        }
        return "0";
    }

    /**
     * Comparator for comparing objects of type {@code Method}
     * <p>
     * m1 > m2 if its return type is assignable from m2 return type (return type is wider)
     */
    private static final BinaryOperator<Method> minByReturnType = BinaryOperator.minBy((m1, m2) -> {
        Class<?> c1 = m1.getReturnType();
        Class<?> c2 = m2.getReturnType();
        return c1.equals(c2) ? 0 : c1.isAssignableFrom(c2) ? 1 : -1;
    });

    /**
     * Method for wrapping array of {@code Method} to array of {@code MyMethod}
     * <p>
     * Returns {@code Map} with key of {@code MyMethod} and value {@code Method}. Methods are abstract or final.
     * If there are more than one method with equal wrappers than for value of map will be choosen
     * with more narrow return type with {@link #minByReturnType}
     *
     * @param methods array of {@code Method}
     * @return {@code Map} with key of wrapper and value {@code Method}
     * @see MyMethod
     */
    private static Map<MyMethod, Method> getMyMethods(final Method[] methods) {
        return Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers())
                        || Modifier.isFinal(method.getModifiers()))
                .collect(Collectors
                        .toMap(MyMethod::new,
                                Function.identity(),
                                minByReturnType));
    }

    /**
     * Method for merging two maps from {@code MyMethod} to {@code Method}
     * <p>
     * Merges elements with {@link #minByReturnType}
     *
     * @param from first map
     * @param to   second map with result of merging
     */
    private static void mergeMaps(Map<MyMethod, Method> from, Map<MyMethod, Method> to) {
        from.forEach((mm, m) -> to.merge(mm, m, minByReturnType));
    }

    /**
     * Method for finding all distinct abstract and final methods of inheritance tree
     * <p>
     * Gets all methods of this {@code token} and its super class with {@link Class#getDeclaredMethods()},
     * {@link Class#getMethods()} and recursion. Then wraps them with {@link #getMyMethods(Method[])} and
     * merge maps with {@link #mergeMaps(Map, Map)}
     *
     * @param token super class or interface
     * @return {@code Collections.emptyMap()} if {@code token == null} or
     * {@code Map} from {@code MyMethod} to {@code Method}
     */
    private Map<MyMethod, Method> findMethodsThroughTree(Class<?> token) {
        if (token == null) {
            return Collections.emptyMap();
        }

        Map<MyMethod, Method> result = getMyMethods(token.getMethods());
        mergeMaps(getMyMethods(token.getDeclaredMethods()), result);
        mergeMaps(findMethodsThroughTree(token.getSuperclass()), result);

        return result;
    }

    /**
     * Method for checking arguments for main()
     *
     * @param args main arguments
     * @return true if {@code args} not null and their length more or equals than 2
     */
    private static boolean checkMainArgs(String[] args) {
        if (args == null) {
            return false;
        }
        if (args.length < 2) {
            return false;
        }
        for (String arg : args) {
            if (arg == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method for executing {@code Implementor} from command line
     *
     * @param args should be of type
     *             "<i>full_super_class_name path_for_directory_of_future_generated_java_file</i>"
     *             (for implementing class) or
     *             "<i>-jar full_super_class_name path_for_directory_of_future_generated_java_file</i>"
     *             (for implementing jar)
     */
    public static void main(String[] args) {

        if (!checkMainArgs(args)) {
            System.out.println("Invalid arguments");
            return;
        }

        try {
            JarImpler implementor = new Implementor();
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } else {
                System.out.println("Invalid amount of arguments");
            }

        } catch (ImplerException e) {
            System.out.println("Impler exception occurred: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Class of this name: " + args[args.length == 2 ? 0 : 1] + " not found");
        }
    }

    /**
     * Custom {@code SimpleFileVisitor} for erasing directories
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        /**
         * Method for erasing visited files
         * @param file {@code Path} to visit
         * @param attrs {@link BasicFileAttributes} attributes
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if erasure failed
         */
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Method for erasing visited directory
         * @param dir {@code Path} of directory to visit
         * @param exc exception while visiting directory
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if erasure of directory failed
         */
        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };


    /**
     * Method for cleaning temporary directory
     * <p>
     * Uses {@link Files#walkFileTree(Path, FileVisitor)} and {@link #DELETE_VISITOR}
     *
     * @param root {@code Path} of the directory
     * @throws ImplerException if root doesn't exist
     */
    public static void clean(final Path root) throws ImplerException {
        if (Files.exists(root)) {
            try {
                Files.walkFileTree(root, DELETE_VISITOR);
            } catch (IOException e) {
                throw new ImplerException("Exception while deleting files");
            }
        }
    }

    /**
     * Method for getting class path from {@code token}
     *
     * @param token super class or interface
     * @return class path of provided {@code token}
     * @throws ImplerException if token couldn't be converted to URI
     */
    private String getClassPath(Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Could not convert token to URI: token = " + token.getSimpleName());
        }
    }

    /**
     * Method for compiling class
     *
     * @param root  directory where will be generated jar file
     * @param token super class or interface
     * @throws ImplerException if java compiler is not provided or its exit code not equals to 0
     */
    public void compileClass(final Path root, Class<?> token) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }

        final String classPath = getClassPath(token);
        final String filePath = getClassFile(token, root).toString();

        final String[] args = Stream.of(filePath,
                         "-encoding", "UTF-8", "-cp", String.join(File.pathSeparator, root.toString(), classPath))
                .toArray(String[]::new);

        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code does not equals to 0: exitCode = " + exitCode);
        }
    }

    /**
     * Method for creating jar manifest
     * <p>
     * Creates manifest with version 1.0 and puts .class files from {@code temp} to jar file
     *
     * @param token   super class or inteface
     * @param temp    temporary directory where .class files will be stored
     * @param jarFile provided {@code Path} of jar file
     * @throws ImplerException if {@link IOException} occurred while writing to jar
     */
    private void createManifest(Class<?> token, Path temp, Path jarFile) throws ImplerException {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");


        try (final JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            final String clazzName = String.format(
                    "%s/%s.class",
                    token.getPackageName().replace('.', '/'),
                    getClassImplName(token)
            );
            output.putNextEntry(new JarEntry(clazzName));
            Files.copy(Paths.get(temp.toString(), clazzName), output);
        } catch (IOException e) {
            throw new ImplerException("Exception while writing to jar");
        }
    }


    /**
     * Method for implementing jar file
     * <p>
     * Implements provided {@code token}. Creates temporary directory, where .class files will be stored.
     * Compiles classes with {@link #compileClass(Path, Class)} and creates manifest for jar with
     * {@link #createManifest(Class, Path, Path)}. Finally cleans temporary directory with
     * {@link #clean(Path)}
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if {@link #isValid(Class)} returns false
     *                         or {@link IOException} occurred while creating temporary directory
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (isValid(token)) {
            createDirectories(jarFile);
            Path temp = null;
            try {
                temp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
                implement(token, temp);
                compileClass(temp, token);
                createManifest(token, temp, jarFile);
            } catch (IOException e) {
                throw new ImplerException("Error while creating temp dir");
            } finally {
                clean(temp);
            }
        } else {
            throw new ImplerException("Token is not supported, token = " + token.getCanonicalName());
        }
    }
}