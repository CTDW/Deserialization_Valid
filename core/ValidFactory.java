package com.seeease.common.plugin.serialValid;


import com.seeease.common.exception.CustomException;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;
import groovy.text.SimpleTemplateEngine;

import java.util.regex.Pattern;

/**
 * 注解校验工厂负责生成对应校验代码
 * @author yzh
 */
public abstract class ValidFactory {

    private enum ValidType{
        NotNull,
        NotBlank,
        Regex;

        private static ValidType parse(String s){
            for (ValidType validType:values()){
                if (s.equals(validType.toString())){
                    return validType;
                }
            }
            return null;
        }
    }

    public static ListBuffer<JCTree.JCStatement> generalValidMethod(JCTree.JCAnnotation annotation, TreeMaker treeMaker, Names names, Name param,JCTree.JCAssign assignExpr){
        ListBuffer<JCTree.JCStatement> testStatement2 = new ListBuffer<>();



        ValidType s = ValidType.parse(annotation.annotationType.toString());
        //创建异常类
        List<JCTree.JCExpression> paramValue = List.nil();
        String message = "";

        for (JCTree.JCExpression var :annotation.args){
            JCTree.JCAssign var1 = (JCTree.JCAssign) var;
            if (var1.lhs.toString().equals("message")){
                message  = var1.rhs.toString();
                break;
            }
        }

        paramValue = paramValue.prepend(treeMaker.Literal(message));

        JCTree.JCNewClass eClass = treeMaker.NewClass(
                null,
                List.nil(),
                treeMaker.Ident(names.fromString("RuntimeException")),
                paramValue,
                null
        );
        //抛出异常
        JCTree.JCThrow exception = treeMaker.Throw(eClass);
        switch (s){
            case NotNull:
                JCTree.JCBinary binary = treeMaker.Binary(JCTree.Tag.EQ, treeMaker.Ident(param), treeMaker.Literal(TypeTag.BOT, null));
                JCTree.JCIf anIf = treeMaker.If(
                        binary, // if语句里面的判断语句
                        exception, // 条件成立的语句
                        null  // 条件不成立的语句
                );
                testStatement2.append(anIf);
                testStatement2.append(treeMaker.Exec(assignExpr));
                return testStatement2;
            case NotBlank:
                System.out.println(1);
                break;
            case Regex:
                String regex = "";
                for (JCTree.JCExpression var :annotation.args){
                    JCTree.JCAssign var1 = (JCTree.JCAssign) var;
                    if (var1.lhs.toString().equals("value")){
                        regex  = var1.rhs.toString();
                        break;
                    }
                }


                List<JCTree.JCExpression> nil = List.nil();
                nil = nil.prepend(treeMaker.Ident(param));
                nil = nil.prepend(treeMaker.Literal(regex));




                JCTree.JCMethodInvocation condition1 = treeMaker.Apply(List.nil(),
                        treeMaker.Select(treeMaker.Ident(names.fromString("Pattern")), names.fromString("matches")),
                        nil);



                JCTree.JCIf varIf = treeMaker.If(
                        treeMaker.Parens(condition1),
                        treeMaker.Exec(assignExpr), // 条件成立的语句
                        null  // 条件不成立的语句
                );
                ListBuffer<JCTree.JCStatement> temp = new ListBuffer<>();
                temp.append(varIf);

                JCTree.JCBinary condition2 = treeMaker.Binary(JCTree.Tag.AND, treeMaker.Binary(JCTree.Tag.NE,treeMaker.Ident(param),treeMaker.Literal(TypeTag.BOT, null)), treeMaker.Unary(JCTree.Tag.NOT,treeMaker.Apply(List.nil(),
                        treeMaker.Select(treeMaker.Ident(param), names.fromString("isEmpty")),
                        List.nil())));

                JCTree.JCIf anIf1 = treeMaker.If(condition2,
                        treeMaker.Block(0, temp.toList()),
                        null);
                testStatement2.append(anIf1);
                testStatement2.append(exception);

                return testStatement2;
            default:
                throw new IllegalStateException("Unexpected value: " + s);
        }

        return null;

    }



}
