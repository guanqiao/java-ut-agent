package com.utagent.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JavaCodeParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaCodeParser.class);

    private final JavaParser javaParser;
    private final FrameworkDetector frameworkDetector;

    public JavaCodeParser() {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        
        SymbolResolver symbolResolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(symbolResolver);
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.javaParser = new JavaParser(config);
        this.frameworkDetector = new FrameworkDetector();
    }

    public JavaCodeParser(List<Path> sourcePaths) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        
        SymbolResolver symbolResolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(symbolResolver);
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.javaParser = new JavaParser(config);
        this.frameworkDetector = new FrameworkDetector();
    }

    public Optional<ClassInfo> parseFile(File file) {
        try {
            ParseResult<CompilationUnit> result = javaParser.parse(file);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                return parseCompilationUnit(result.getResult().get());
            }
        } catch (FileNotFoundException e) {
            logger.error("File not found: {}", file.getAbsolutePath(), e);
        }
        return Optional.empty();
    }

    public Optional<ClassInfo> parseCode(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return parseCompilationUnit(result.getResult().get());
        }
        return Optional.empty();
    }

    public List<ClassInfo> parseDirectory(File directory) {
        List<ClassInfo> classes = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".java"));
            if (files != null) {
                for (File file : files) {
                    parseFile(file).ifPresent(classes::add);
                }
                File[] subDirs = directory.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        classes.addAll(parseDirectory(subDir));
                    }
                }
            }
        }
        return classes;
    }

    private Optional<ClassInfo> parseCompilationUnit(CompilationUnit cu) {
        String packageName = cu.getPackageDeclaration()
            .map(pd -> pd.getNameAsString())
            .orElse("");

        List<String> imports = new ArrayList<>();
        cu.getImports().forEach(imp -> imports.add(imp.getNameAsString()));

        Optional<ClassOrInterfaceDeclaration> classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (classDecl.isPresent()) {
            return Optional.of(buildClassInfo(classDecl.get(), packageName, imports));
        }

        Optional<EnumDeclaration> enumDecl = cu.findFirst(EnumDeclaration.class);
        if (enumDecl.isPresent()) {
            return Optional.of(buildEnumInfo(enumDecl.get(), packageName, imports));
        }

        Optional<RecordDeclaration> recordDecl = cu.findFirst(RecordDeclaration.class);
        if (recordDecl.isPresent()) {
            return Optional.of(buildRecordInfo(recordDecl.get(), packageName, imports));
        }

        return Optional.empty();
    }

    private ClassInfo buildClassInfo(ClassOrInterfaceDeclaration decl, String packageName, List<String> imports) {
        String className = decl.getNameAsString();
        String fullName = packageName.isEmpty() ? className : packageName + "." + className;

        List<MethodInfo> methods = new ArrayList<>();
        decl.getMethods().forEach(m -> methods.add(buildMethodInfo(m)));

        List<FieldInfo> fields = new ArrayList<>();
        decl.getFields().forEach(f -> fields.addAll(buildFieldInfo(f)));

        List<AnnotationInfo> annotations = new ArrayList<>();
        decl.getAnnotations().forEach(a -> annotations.add(buildAnnotationInfo(a)));

        String superClass = !decl.getExtendedTypes().isEmpty() 
            ? decl.getExtendedTypes().get(0).getNameAsString() 
            : null;

        List<String> interfaces = new ArrayList<>();
        decl.getImplementedTypes().forEach(i -> interfaces.add(i.getNameAsString()));

        return new ClassInfo(
            packageName,
            className,
            fullName,
            methods,
            fields,
            annotations,
            imports,
            superClass,
            interfaces,
            decl.isInterface(),
            false,
            false,
            new HashMap<>()
        );
    }

    private ClassInfo buildEnumInfo(EnumDeclaration decl, String packageName, List<String> imports) {
        String className = decl.getNameAsString();
        String fullName = packageName.isEmpty() ? className : packageName + "." + className;

        List<MethodInfo> methods = new ArrayList<>();
        decl.getMethods().forEach(m -> methods.add(buildMethodInfo(m)));

        List<AnnotationInfo> annotations = new ArrayList<>();
        decl.getAnnotations().forEach(a -> annotations.add(buildAnnotationInfo(a)));

        return new ClassInfo(
            packageName,
            className,
            fullName,
            methods,
            new ArrayList<>(),
            annotations,
            imports,
            "Enum",
            new ArrayList<>(),
            false,
            true,
            false,
            new HashMap<>()
        );
    }

    private ClassInfo buildRecordInfo(RecordDeclaration decl, String packageName, List<String> imports) {
        String className = decl.getNameAsString();
        String fullName = packageName.isEmpty() ? className : packageName + "." + className;

        List<MethodInfo> methods = new ArrayList<>();
        decl.getMethods().forEach(m -> methods.add(buildMethodInfo(m)));

        List<AnnotationInfo> annotations = new ArrayList<>();
        decl.getAnnotations().forEach(a -> annotations.add(buildAnnotationInfo(a)));

        List<ParameterInfo> recordComponents = new ArrayList<>();
        decl.getParameters().forEach(p -> 
            recordComponents.add(new ParameterInfo(p.getNameAsString(), p.getTypeAsString()))
        );

        return new ClassInfo(
            packageName,
            className,
            fullName,
            methods,
            new ArrayList<>(),
            annotations,
            imports,
            "Record",
            new ArrayList<>(),
            false,
            false,
            true,
            new HashMap<>()
        );
    }

    private MethodInfo buildMethodInfo(MethodDeclaration method) {
        String name = method.getNameAsString();
        String returnType = method.getType().asString();

        List<ParameterInfo> parameters = new ArrayList<>();
        method.getParameters().forEach(p -> 
            parameters.add(new ParameterInfo(
                p.getNameAsString(),
                p.getTypeAsString(),
                p.isVarArgs()
            ))
        );

        List<AnnotationInfo> annotations = new ArrayList<>();
        method.getAnnotations().forEach(a -> annotations.add(buildAnnotationInfo(a)));

        String body = method.getBody().map(b -> b.toString()).orElse(null);

        int lineNumber = method.getBegin().map(p -> p.line).orElse(0);
        int endLineNumber = method.getEnd().map(p -> p.line).orElse(0);

        List<String> thrownExceptions = new ArrayList<>();
        method.getThrownExceptions().forEach(e -> thrownExceptions.add(e.asString()));

        return new MethodInfo(
            name,
            returnType,
            parameters,
            annotations,
            body,
            lineNumber,
            endLineNumber,
            thrownExceptions,
            method.isStatic(),
            method.isPrivate(),
            method.isProtected(),
            method.isPublic(),
            method.isAbstract(),
            method.isFinal()
        );
    }

    private List<FieldInfo> buildFieldInfo(FieldDeclaration field) {
        List<FieldInfo> fields = new ArrayList<>();
        
        List<AnnotationInfo> annotations = new ArrayList<>();
        field.getAnnotations().forEach(a -> annotations.add(buildAnnotationInfo(a)));

        for (VariableDeclarator var : field.getVariables()) {
            fields.add(new FieldInfo(
                var.getNameAsString(),
                field.getElementType().asString(),
                annotations,
                field.isStatic(),
                field.isFinal(),
                field.isPrivate(),
                field.isProtected(),
                field.isPublic()
            ));
        }
        
        return fields;
    }

    private AnnotationInfo buildAnnotationInfo(AnnotationExpr annotation) {
        String name = annotation.getNameAsString();
        Map<String, Object> attributes = new HashMap<>();

        if (annotation instanceof NormalAnnotationExpr normalAnnotation) {
            for (MemberValuePair pair : normalAnnotation.getPairs()) {
                attributes.put(pair.getNameAsString(), pair.getValue().toString());
            }
        } else if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
            attributes.put("value", singleMember.getMemberValue().toString());
        }

        return new AnnotationInfo(name, attributes);
    }

    public FrameworkDetector getFrameworkDetector() {
        return frameworkDetector;
    }
}
