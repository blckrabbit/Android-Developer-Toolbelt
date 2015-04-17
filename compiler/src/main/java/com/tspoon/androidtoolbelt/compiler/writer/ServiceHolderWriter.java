package com.tspoon.androidtoolbelt.compiler.writer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.tspoon.androidtoolbelt.compiler.ToolbeltProcessor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import timber.log.Timber;

public class ServiceHolderWriter implements SourceWriter {

    private static final String CLASS_NAME = "ServiceHolderImpl";

    private static final String FIELD_SERVICES = "SERVICES";
    private static final String FIELD_CONNECTIONS = "CONNECTIONS";

    @Override
    public String getFileName() {
        return PACKAGE + "." + CLASS_NAME;
    }

    @Override
    public void writeSource(Writer writer) throws IOException {
        TypeSpec typeSpec = TypeSpec.classBuilder(CLASS_NAME)
                .addSuperinterface(ClassName.get(PACKAGE, "ServiceHolder"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(createFieldServices())
                .addField(createFieldConnections())
                .addMethod(createStartServices())
                .addMethod(createStopServices())
                .build();

        JavaFile javaFile = JavaFile.builder(PACKAGE, typeSpec)
                .addFileComment("Generated by MemoryServiceWriter.java. Do not modify!")
                .build();

        javaFile.writeTo(writer);
    }

    private FieldSpec createFieldServices() {
        TypeName type = ParameterizedTypeName.get(ClassName.get(List.class), ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(Service.class))));

        CodeBlock.Builder builder = CodeBlock.builder()
                .add("new $T(){{\n", ParameterizedTypeName.get(ClassName.get(ArrayList.class), ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(Service.class)))));

        for (int i = 1; i <= ToolbeltProcessor.NUMBER_OF_SERVICES; i++) {
            builder.addStatement("add($T.class)", ClassName.bestGuess("MemoryService" + i));
        }
        builder.add("}}");

        return FieldSpec.builder(type, FIELD_SERVICES, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(builder.build().toString())
                .build();
    }

    private FieldSpec createFieldConnections() {
        TypeName type = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(PACKAGE, "MemoryServiceConnection"));
        return FieldSpec.builder(type, FIELD_CONNECTIONS, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T<>()", ArrayList.class)
                .build();
    }


    private MethodSpec createStartServices() {
        ClassName memoryServiceConnection = ClassName.get(PACKAGE, "MemoryServiceConnection");

        CodeBlock code = CodeBlock.builder()
                .beginControlFlow("for($T service: $L)", ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Service.class)), FIELD_SERVICES)
                .addStatement("$T intent = new $T(context, service)", Intent.class, Intent.class)
                .addStatement("context.startService(intent)")
                .add("\n")
                .addStatement("$T connection = new $T()", memoryServiceConnection, memoryServiceConnection)
                .addStatement("context.bindService(intent, connection, Context.BIND_AUTO_CREATE)")
                .endControlFlow()
                .build();

        return MethodSpec.methodBuilder("startServices")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Context.class), "context")
                .addCode(code)
                .build();
    }

    private MethodSpec createStopServices() {
        CodeBlock code = CodeBlock.builder()
                .addStatement("$T.d(\"Stopping Services... \" + $L.size() + \" connections found.\")", Timber.class, FIELD_CONNECTIONS)
                .beginControlFlow("while($L.size() > 0)", FIELD_CONNECTIONS)
                .addStatement("$L.remove(0).stopService()", FIELD_CONNECTIONS)
                .endControlFlow()
                .build();

        return MethodSpec.methodBuilder("stopServices")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addCode(code)
                .build();
    }


}
