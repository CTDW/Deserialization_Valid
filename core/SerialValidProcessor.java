package com.seeease.common.plugin.serialValid;

import com.google.auto.service.AutoService;
import com.seeease.common.exception.CustomException;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yzh
 */
@SupportedAnnotationTypes({"com.seeease.common.plugin.serialValid.NotNull",
                            "com.seeease.common.plugin.serialValid.Regex"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class SerialValidProcessor extends AbstractProcessor{

    private JavacTrees trees;
    //语法树构建
    private TreeMaker treeMaker;
    private Names names;
    private int count = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //获取项目环境中的所有类节点数
        this.trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (count > 1){
            return true;
        }
        annotations.forEach(A->{
            Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(A);
            set.forEach(item->{
                //1.获取注解作用的成员变量的拥有类的节点树
                JCTree jcTree = trees.getTree(item);
                //获取该注解
                Type aType = ((Symbol) A).type;
                JCTree.JCAnnotation jcAnnotation = ((JCTree.JCVariableDecl) jcTree)
                        .mods.annotations
                        .stream()
                        .filter(a -> a.type.equals(aType))
                        .collect(Collectors.toList())
                        .get(0);
                //2.解析当前类语法树
                JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) jcTree;
                JCTree classJcTree = trees.getTree(jcVariableDecl.sym.owner);
                //导包

                importPackage(jcTree);


                classJcTree.accept(new TreeTranslator() {
                    @Override
                    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                        String fieldName = jcVariableDecl.getName().toString();
                        String targetFieldSetMethod = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        JCTree.JCMethodDecl targetFieldSetMethodExit = getTargetFieldSetMethod(jcClassDecl.defs, targetFieldSetMethod);
                        if (targetFieldSetMethodExit == null){
                            JCTree.JCMethodDecl jcMethodDecl = generalFieldSetMethod(targetFieldSetMethod, jcVariableDecl.name, jcVariableDecl.vartype,jcAnnotation);
                            jcClassDecl.defs =  jcClassDecl.defs.prepend(jcMethodDecl);
                        }else {


                        }
                        super.visitClassDef(jcClassDecl);
                    }

                });
            });
        });
        count++;
        return true;
    }

    private JCTree.JCMethodDecl getTargetFieldSetMethod(List<JCTree> list,String targetFieldName){
        for (JCTree var1 : list) {
            if (var1 instanceof JCTree.JCMethodDecl){
                Name name = ((JCTree.JCMethodDecl) var1).getName();
                if (name.toString().equals(targetFieldName)){
                    return (JCTree.JCMethodDecl)var1;
                }
            }
        }
        return null;
    }

    private JCTree.JCMethodDecl generalFieldSetMethod(String targetFieldName, Name fieldName, JCTree.JCExpression type,JCTree.JCAnnotation jcAnnotation){
        List<JCTree.JCVariableDecl> params = List.nil();
        //创建方法名表达式
        JCTree.JCVariableDecl jcVariableDecl = generalSetMethodParam(fieldName,type);
        params = params.prepend(jcVariableDecl);
        //创建方法体表达式
        JCTree.JCBlock jcBlock = generalSetMethodBodyBlock(fieldName,jcAnnotation);

        return  treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC),
                names.fromString(targetFieldName),
                treeMaker.TypeIdent(TypeTag.VOID),
                List.nil(),
                params,
                List.nil(),
                jcBlock,
                null);
    }


    private JCTree.JCVariableDecl generalSetMethodParam(Name fieldName, JCTree.JCExpression type){
        return  treeMaker.VarDef(treeMaker.Modifiers(Flags.PARAMETER), // 访问标识
                fieldName, // 名称
                type, // 类型
                null);
    }

    private JCTree.JCBlock generalSetMethodBodyBlock(Name param,JCTree.JCAnnotation jcAnnotation){
        //赋值左等式
        JCTree.JCFieldAccess assignLeft = treeMaker.Select(treeMaker.Ident(names.fromString("this")), param);
        //赋值右等式
        JCTree.JCIdent assignRight = treeMaker.Ident(param);
        //赋值语句
        JCTree.JCAssign assignExpr = treeMaker.Assign(assignLeft, assignRight);
        ListBuffer<JCTree.JCStatement> jcStatement = ValidFactory.generalValidMethod(jcAnnotation, treeMaker, names, param,assignExpr);
        return treeMaker.Block(0, jcStatement.toList());
    }


    private void importPackage(JCTree jcTree){
        TreePath path = trees.getPath(((JCTree.JCVariableDecl)jcTree).sym.owner);
        JCTree.JCFieldAccess exceptionImpor = treeMaker.Select(treeMaker.Select(treeMaker.Ident(names.fromString("java")), names.fromString("lang")), names.fromString("RuntimeException"));
        JCTree.JCImport anImport = treeMaker.Import(exceptionImpor, false);
        JCTree.JCFieldAccess exceptionImpor1 = treeMaker.Select(treeMaker.Select(treeMaker.Select(treeMaker.Ident(names.fromString("java")), names.fromString("util")), names.fromString("regex")),names.fromString("Pattern"));
        JCTree.JCImport anImport1 = treeMaker.Import(exceptionImpor1, false);
        ((JCTree.JCCompilationUnit)path.getCompilationUnit()).defs = ((JCTree.JCCompilationUnit)path.getCompilationUnit()).defs.prepend(anImport);
        ((JCTree.JCCompilationUnit)path.getCompilationUnit()).defs = ((JCTree.JCCompilationUnit)path.getCompilationUnit()).defs.prepend(anImport1);

    }

}