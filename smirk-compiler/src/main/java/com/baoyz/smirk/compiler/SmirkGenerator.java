/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 baoyongzhang <baoyz94@gmail.com>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.baoyz.smirk.compiler;

import com.baoyz.smirk.Smirk;
import com.baoyz.smirk.SmirkManager;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.type.TypeKind.VOID;

/**
 * Created by baoyz on 15/10/28.
 */
public class SmirkGenerator extends FilerGenerator {

    public SmirkGenerator(Filer filer) {
        super(filer);
    }

    @Override
    public TypeSpec onCreateTypeSpec(TypeElement element) {
        String qualifiedName = element.getQualifiedName().toString();
        String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String className = qualifiedName.substring(packageName.length() + 1);

        ClassName extensionInterface = ClassName.get(packageName, className);
        TypeName managerInterface = ParameterizedTypeName.get(ClassName.get(SmirkManager.class), extensionInterface);
        TypeSpec.Builder builder = TypeSpec.classBuilder(className + Smirk.MANAGER_SUFFIX)
                .addSuperinterface(extensionInterface)
                .addSuperinterface(managerInterface)
                .addModifiers(Modifier.PUBLIC);

        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfExtensions = ParameterizedTypeName.get(list, extensionInterface);

        FieldSpec listField = FieldSpec
                .builder(listOfExtensions, "mList", Modifier.PRIVATE)
                .build();

        MethodSpec putAll = MethodSpec.methodBuilder("putAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.VOID)
                .addParameter(listOfExtensions, "list")
                .addStatement("mList = list").build();

        builder.addField(listField);
        builder.addMethod(putAll);

        // implements extension method
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e instanceof ExecutableElement) {
                ExecutableElement ee = (ExecutableElement) e;

                // TODO return value?
                boolean isVoid = false;
                if (ee.getReturnType().getKind().equals(VOID)) {
                    isVoid = true;
                }
                MethodSpec.Builder extensionMethodBuilder = MethodSpec
                        .methodBuilder(ee.getSimpleName().toString())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(ee.getReturnType()));

                if (ee.getThrownTypes() != null && ee.getThrownTypes().size() > 0) {
                    List<TypeName> exceptions = new ArrayList<>();
                    for (TypeMirror type : ee.getThrownTypes()) {
                        TypeName typeName = TypeName.get(type);
                        exceptions.add(typeName);
                    }
                    extensionMethodBuilder.addExceptions(exceptions);
                }

                List<? extends VariableElement> parameters = ee.getParameters();
                String name = "v";
                StringBuilder paramsString = new StringBuilder();
                int index = 0;
                for (VariableElement param : parameters) {
                    String paramName = name + index;
                    extensionMethodBuilder.addParameter(TypeName.get(param.asType()), paramName, param.getModifiers().toArray(new Modifier[param.getModifiers().size()]));
                    index++;
                    paramsString.append(paramName);
                    if (index < parameters.size()) {
                        paramsString.append(", ");
                    }
                }

                extensionMethodBuilder.beginControlFlow("for ($T item : mList)", extensionInterface)
                        .addStatement("item.$L($L)", ee.getSimpleName(), paramsString.toString())
                        .endControlFlow();

                builder.addMethod(extensionMethodBuilder.build());

            }
        }

        return builder.build();
    }
}
