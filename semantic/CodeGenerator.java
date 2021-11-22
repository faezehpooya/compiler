package semantic;

import lexical.Symbol;
import semantic.ast.SymbolEntity;
import semantic.ast.expression.Expression;
import semantic.ast.expression.binary.*;
import semantic.ast.expression.constant.DoubleConst;
import semantic.ast.expression.constant.IntegerConst;
import semantic.ast.expression.constant.StringConst;
import semantic.ast.expression.unary.Negative;
import syntax.Lexical;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CodeGenerator implements syntax.CodeGenerator {
    private Lexical lexical;
    private Deque<Object> semanticStack;

    private SymbolTable currentSymbolTable = new SymbolTable();

    private Dictionary llvmTypes = new Hashtable();
    private Dictionary llvmAlign = new Hashtable();
    private ArrayList<String> llvmCode = new ArrayList<String>();
    private int llvmRegNum = 0;
    private int ifLableNum = 0;
    private int whileLabelNum = 0;
    private int whileCondLine = 0;


    private SymbolEntity id_asssign;

    private void addLlvmCode(String line){
        llvmCode.add(line);
    }

    private void setLlvmTypes(){
        this.llvmTypes.put("integer", "i32");
        this.llvmTypes.put("long", "i64");
        this.llvmTypes.put("char", "i8");
        this.llvmTypes.put("real", "float");
        this.llvmTypes.put("boolean", "i8");
        this.llvmTypes.put("string", "i8");
        this.llvmTypes.put("array", "i64");
        this.llvmTypes.put("void", "void");
    }

    private void setLlvmAlign(){
        this.llvmAlign.put("integer", "4");
        this.llvmAlign.put("long", "8");
        this.llvmAlign.put("char", "1");
        this.llvmAlign.put("real", "4");
        this.llvmAlign.put("boolean", "1");
        this.llvmAlign.put("string", "4");
        this.llvmAlign.put("array", "16");
    }

    public CodeGenerator(Lexical lexical) {
        this.lexical = lexical;
        semanticStack = new ArrayDeque<>();
        setLlvmTypes();
        setLlvmAlign();
        setllvmDefaultFunc();
    }

    private void setllvmDefaultFunc() {
        String l = "@.str.fuck = private unnamed_addr constant [3 x i8] c\"%d\\00\", align 1";
        addLlvmCode(l);
        l = "@.str.fuck.1 = private unnamed_addr constant [3 x i8] c\"%c\\00\", align 1";
        addLlvmCode(l);
        l = "declare i32 @printf(i8*, ...)";
        addLlvmCode(l);
        l  = "declare i32 @scanf(i8*, ...)";
        addLlvmCode(l);
        l = "@.str.fuck.2 = private unnamed_addr constant [3 x i8] c\"%s\\00\", align 1";
        addLlvmCode(l);
        l = "@.str.fuck.3 = private unnamed_addr constant [3 x i8] c\"%f\\00\", align 1";
        addLlvmCode(l);
        l = "declare double @llvm.fabs.f64(double)";
        addLlvmCode(l);
        l = "declare i64 @strlen(i8*)";
        addLlvmCode(l);
        l = "declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)";
        addLlvmCode(l);
    }

    public Expression getResult() {
        return (Expression) semanticStack.getFirst();
    }

    public void generateLlvmFile(){
        try(FileWriter fw = new FileWriter("/home/motahareh/Documents/University/Compiler/Project/Dadash/90110172_89108139/Compiler/CompilerProject/src/llvm.ll", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {

            for (int i = 0; i < llvmCode.size(); i++){
                out.println(llvmCode.get(i));
            }
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    public String cast(String result_type, String type_id, String value) {
        String llvmLine = "";
        if (result_type.equals("integer") || result_type.equals("long")) {
            if (type_id.equals("char")){
                llvmLine += "%conv" + llvmRegNum + " = trunc i32 " + value + " to i8 \n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
            } else if (type_id.equals("real")) {
                llvmLine += "%conv" + llvmRegNum + " = sitofp i32 " + value + " to float \n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
            } else if (type_id.equals("boolean")) {
                llvmLine += "%tobool" + llvmRegNum + " = icmp ne i32 " + value + ", 0 \n";
                llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                value = "%frombool" + llvmRegNum;
                llvmRegNum += 1;
            }
        } else if (result_type.equals("boolean")) {
            llvmLine += "%tobool" + llvmRegNum + " = turnc i8 " + value + "to i1\n";
            if (type_id.equals("float")){
                llvmLine += "%conv" + llvmRegNum + " = uitofp i1 %tobool" + llvmRegNum + " to " + llvmTypes.get(type_id) + "\n";
            }
            else {
                llvmLine += "%conv" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to " + llvmTypes.get(type_id) + "\n";
            }
            value = "%conv" + llvmRegNum;
            llvmRegNum += 1;

        } else if (result_type.equals("char")) {
            if (type_id.equals("integer") || type_id.equals("long")) {
                llvmLine += "%conv" + llvmRegNum + " = sext i8 " + value + " to i32\n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
            } else if (type_id.equals("real")) {
                llvmLine += "%conv" + llvmRegNum + " = sext i8 " + value + " to i32\n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
                llvmLine += "%conv" + llvmRegNum + " = sitofp i32 " + value + " to float\n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
            } else if (type_id.equals("boolean")) {
                llvmLine += "%tobool" + llvmRegNum + " = icmp ne i8 " + value + ", 0 \n";
                llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                value = "%frombool" + llvmRegNum;
                llvmRegNum += 1;
            }

        } else if (result_type.equals("real")) {
            if (type_id.equals("integer") || type_id.equals("long") || type_id.equals("char")) {
                llvmLine += "%conv" + llvmRegNum + " = fptosi float " + value + " to " + llvmTypes.get(type_id) + "\n";
                value = "%conv" + llvmRegNum;
                llvmRegNum += 1;
            } else if (type_id.equals("boolean")){
                llvmLine += "%tobool" + llvmRegNum + " = fcmp une float " + value + ", 0.000000e+00\n";
                llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                value = "%frombool" + llvmRegNum;
                llvmRegNum += 1;
            }
        }

        addLlvmCode(llvmLine);

        return value.substring(1);

    }


    public void doSemantic(String sem) {
        Boolean debug = true;
        String llvmLine = "";


        Iterator iteratorVals = semanticStack.iterator();

        switch (sem) {
                
            case "pushFuncName":
                if(debug){
                    System.out.println("CG: pushFuncName");
                }
                SymbolEntity funcSymbol = new SymbolEntity((String)lexical.currentToken().getValue());
                funcSymbol.setType("function");
                currentSymbolTable.add(funcSymbol);
                semanticStack.push(funcSymbol);
                SymbolTable funcSymbolTable = new SymbolTable();
                funcSymbolTable.setFatherSymbolTable(currentSymbolTable);
                currentSymbolTable = funcSymbolTable;
                semanticStack.push((String)lexical.currentToken().getValue());
                break;
                
            case "pushId":
                if(debug){
                    System.out.println("CG: pushId");
                }
                SymbolEntity entity = new SymbolEntity((String) lexical.currentToken().getValue());
                semanticStack.push(entity);
                break;
                
            case "pushConst":
                if(debug){
                    System.out.println("CG: pushConst");
                }
                semanticStack.push(lexical.currentToken().getValue() + "");
                break;

            case "pushExprId":
                if(debug){
                    System.out.println("CG: pushExprId");
                }
                SymbolEntity foundSymbol = currentSymbolTable.findSymbol((String) lexical.currentToken().getValue());
                if (foundSymbol == null){
                    entity = new SymbolEntity((String) lexical.currentToken().getValue());
                    semanticStack.push(entity);
                    System.out.println(lexical.currentToken().getValue() + " Id not found.");
                    //end the program
                }
                else {
                    semanticStack.push(foundSymbol);
                }
                break;
                
            case "pushType":
                if(debug){
                    System.out.println("CG: pushType");
                }
                semanticStack.push((String) lexical.currentToken().getToken());
                break;

            case "pushIdSTMT": //create symbol without adding to symbol table
                if(debug){
                    System.out.println("CG: pushIdSTMT");
                }
                boolean found = currentSymbolTable.find((String) lexical.currentToken().getValue());
                if (!found){
                    entity = new SymbolEntity((String) lexical.currentToken().getValue());
                }
                else {
                    entity = (SymbolEntity) currentSymbolTable.findSymbol((String) lexical.currentToken().getValue());
                }
                semanticStack.push(entity);
                break;
                
            case "alloca":
                if(debug){
                    System.out.println("CG: alloca");
                }
                String type = (String) semanticStack.pop();
                SymbolEntity id = (SymbolEntity) semanticStack.pop();
                found = currentSymbolTable.find(id.name);
                if(found){
                    System.out.println("Symbol has already been declared.");
                }
                id.type = type;
                if(type.equals("string")){
                    System.out.println("goz nakhor");
                }
                else {
                    llvmLine += "%" + id.name + " = alloca " + llvmTypes.get(id.type) + ", align " + llvmAlign.get(id.type) + "\n";
                }
                currentSymbolTable.add(id);
                addLlvmCode(llvmLine);
                id_asssign = id;
                break;

            case "pushIdAssign":
                if(debug){
                    System.out.println("CG: pushIdAssign");
                }
                semanticStack.push(id_asssign);
                break;

            case "assignment":
                if(debug){
                    System.out.println("CG: assignment");
                }

                Object exprResult = semanticStack.pop();
                id = null;
                try {
                    id = (SymbolEntity) semanticStack.pop();
                } catch (Exception e0) {
                    System.out.println("id was not defined.\n");
                }

                found = currentSymbolTable.find(id.name);
                if (!found) {
                    System.out.println("id was not defined.\n");
                }

                String type_id = id.type;
                if (type_id.equals("array")) {
                    type_id = id.arrayElementType;
                }
                String value = "";
                if (exprResult instanceof SymbolEntity) {
                    String result_type = ((SymbolEntity) exprResult).type;
                    if (result_type.equals("array")) {
                        result_type = ((SymbolEntity) exprResult).arrayElementType;
                    }
                    value = "%" + ((SymbolEntity) exprResult).name;
                    found = currentSymbolTable.find(((SymbolEntity) exprResult).name);
                    if (found) {
                        llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(result_type) + ", " +
                                llvmTypes.get(result_type) + "* %" + ((SymbolEntity) exprResult).name +
                                ", align " + llvmAlign.get(result_type) + " \n";
                        value = "%t" + llvmRegNum;
                        llvmRegNum += 1;
                    }
                    if (result_type.equals("integer") || result_type.equals("long")) {
                        if (type_id.equals("char")){
                            llvmLine += "%conv" + llvmRegNum + " = trunc i32 " + value + " to i8 \n";
                            value = "%conv" + llvmRegNum;
                            llvmRegNum += 1;
                        } else if (type_id.equals("real")) {
                            llvmLine += "%conv" + llvmRegNum + " = sitofp i32 " + value + " to float \n";
                            value = "%conv" + llvmRegNum;
                            llvmRegNum += 1;
                        } else if (type_id.equals("boolean")) {
                            llvmLine += "%tobool" + llvmRegNum + " = icmp ne i32 " + value + ", 0 \n";
                            llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                            value = "%frombool" + llvmRegNum;
                            llvmRegNum += 1;
                        }
                    } else if (result_type.equals("boolean")) {
                        llvmLine += "%tobool" + llvmRegNum + " = turnc i8 " + value + "to i1\n";
                        if (type_id.equals("float")){
                            llvmLine += "%conv" + llvmRegNum + " = uitofp i1 %tobool" + llvmRegNum + " to " + llvmTypes.get(type_id) + "\n";
                        }
                        else {
                            llvmLine += "%conv" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to " + llvmTypes.get(type_id) + "\n";
                        }
                        value = "%conv" + llvmRegNum;
                        llvmRegNum += 1;

                    } else if (result_type.equals("char")) {
                        if (type_id.equals("integer") || type_id.equals("long")) {
                            llvmLine += "%conv" + llvmRegNum + " = sext i8 " + value + " to i32\n";
                            value = "%conv" + llvmRegNum;
                            llvmRegNum += 1;
                        } else if (type_id.equals("real")) {
                            llvmLine += "%conv" + llvmRegNum + " = sitofp i8 " + value + " to float\n";
                            value = "%conv" + llvmRegNum;
                            llvmRegNum += 1;
                        } else if (type_id.equals("boolean")) {
                            llvmLine += "%tobool" + llvmRegNum + " = icmp ne i8 " + value + ", 0 \n";
                            llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                            value = "%frombool" + llvmRegNum;
                            llvmRegNum += 1;
                        }

                    } else if (result_type.equals("real")) {
                        if (type_id.equals("integer") || type_id.equals("long") || type_id.equals("char")) {
                            llvmLine += "%conv" + llvmRegNum + " = fptosi float " + value + " to " + llvmTypes.get(type_id) + "\n";
                            value = "%conv" + llvmRegNum;
                            llvmRegNum += 1;
                        } else if (type_id.equals("boolean")){
                            llvmLine += "%tobool" + llvmRegNum + " = fcmp une float " + value + ", 0.000000e+00\n";
                            llvmLine += "%frombool" + llvmRegNum + " = zext i1 %tobool" + llvmRegNum + " to i8\n";
                            value = "%frombool" + llvmRegNum;
                            llvmRegNum += 1;
                        }
                    }


                } else {
                    String b = value;
                    value = (String) exprResult;
                    if (value.equals("true")) {
                        value = "1";
                        b = "1";
                    } else if (value.equals("false")) {
                        value = "0";
                        b = "0";
                    } else {
                        try {
                            int a = Integer.parseInt(value);
                            b = a > 0 ? "1" : "0";

                        } catch (Exception e2) {
                            try {
                                float f = Float.parseFloat(value);
                                llvmRegNum += 1;
                                if (!type_id.equals("real")) {
                                    int a = (int) f;
                                    value = a + "";
                                } else {
                                    llvmLine += "%E" + llvmRegNum + " = call fast double @llvm.fabs.f64(double " + f + ")\n";
                                    llvmLine += "%F" + llvmRegNum +  "= fptrunc double %E" + llvmRegNum + " to float\n";
                                    value = "%F" + llvmRegNum;
                                    llvmRegNum += 1;
                                }
                                b = f > 0.0 ? "1" : "0";
                            } catch (Exception e3) {
                                if (type_id.equals("string")){
                                    id.value = value;
                                    llvmLine += "%" + id.name + " = alloca [" + (value.length() + 1) + " x i8] , align 1\n";
                                    llvmLine += "%t" + llvmRegNum + " = bitcast [" + (value.length() + 1) + " x i8]* %" + id.name + " to i8*\n";
                                    String line = "@.main.s" + llvmRegNum + " = private unnamed_addr constant [" + (value.length() + 1) + " x i8] c\"" + value + "\\00\", align 1\n";
                                    llvmCode.add(0, line);

                                    llvmLine += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* %t" + llvmRegNum + ", i8* getelementptr inbounds " + "([" + (value.length() + 1) + " x i8], [ " +
                                            (value.length() + 1) + " x i8]* @.main.s" + llvmRegNum + ", i32 0, i32 0), i64 " + (value.length() + 1) + ", i32 1, i1 false) \n";
                                    llvmRegNum += 1;
                                    addLlvmCode(llvmLine);
                                    break;

                                }
                                else if (id.type.equals("array") && id.arrayElementType.equals("char")){
                                    String dimString = "";
                                    for(int i = 0; i < id.dimention.size(); i++){
                                        dimString += "[" + id.dimention.get(i) + " x ";
                                    }
                                    dimString += llvmTypes.get(id.arrayElementType);
                                    for(int i = 0; i < id.dimention.size(); i++){
                                        dimString += "]";
                                    }
                                    llvmLine += "%t" + llvmRegNum + " = bitcast " + dimString + "* %" + id.name + " to i8*\n";
                                    String line = "@.main.s" + llvmRegNum + " = private unnamed_addr constant [" + (value.length() + 1) + " x i8] c\"" + value + "\\00\", align 1\n";
                                    llvmCode.add(0, line);

                                    llvmLine += "call void @llvm.memcpy.p0i8.p0i8.i64(i8* %t" + llvmRegNum + ", i8* getelementptr inbounds " + "([" + (value.length() + 1) + " x i8], [ " +
                                            (value.length() + 1) + " x i8]* @.main.s" + llvmRegNum + ", i32 0, i32 0), i64 " + (value.length() + 1) + ", i32 1, i1 false) \n";
                                    llvmRegNum += 1;
                                    addLlvmCode(llvmLine);
                                    break;
                                }
                                value = (int) value.charAt(0) + "";
                                b = "1";
                            }
                        }
                    }
                    if (type_id.equals("boolean")){
                        value = b;
                    }

                }

                llvmLine += "store " + llvmTypes.get(type_id) + " " + value + ", " + llvmTypes.get(type_id) + "* %" + id.name + ", align " + llvmAlign.get(type_id) + "\n";
                addLlvmCode(llvmLine);

                break;
                
            case "array":
                if(debug){
                    System.out.println("CG: array");
                }
                type = (String) semanticStack.pop();
                String top = (String) semanticStack.pop();
                SymbolEntity array = new SymbolEntity("tempNameArray");
                array.arrayElementType = type;
                array.type = "array";
                String dimString = "";
                while (!top.equals("array")){
                    array.dimention.add(0, Integer.parseInt(top));
                    top = (String) semanticStack.pop();
                }
                for(int i = 0; i < array.dimention.size(); i++){
                    dimString += "[" + array.dimention.get(i) + " x ";
                }
                dimString += llvmTypes.get(array.arrayElementType);
                for(int i = 0; i < array.dimention.size(); i++){
                    dimString += "]";
                }
                SymbolEntity arrayid = (SymbolEntity) semanticStack.pop();
                array.name = arrayid.name;
                llvmLine += "%" + array.name + " = alloca " + dimString + " , align " + llvmAlign.get(array.type) ;
                addLlvmCode(llvmLine);
                currentSymbolTable.add(array);
                break;

            case "arrayAddress":
                if(debug){
                    System.out.println("CG: arrayAddress");
                }
                String varName;
                System.out.println(semanticStack);
                Object topStack = semanticStack.pop();
                ArrayList<Integer> dimension = new ArrayList<>();
                while(!((topStack instanceof String) && topStack.equals("["))){
                    System.out.println(topStack);
                    if (topStack instanceof SymbolEntity) {
                        if (currentSymbolTable.find(((SymbolEntity) topStack).name)) {
                            varName = (String) ((SymbolEntity) topStack).value;
                        } else {
                            varName = ((SymbolEntity) topStack).name;
                        }
                    } else {
                        varName = (String) topStack;
                    }
                    dimension.add(Integer.parseInt(varName));
                    topStack = semanticStack.pop();
                }
                topStack = (SymbolEntity) semanticStack.pop();
                System.out.println(((SymbolEntity) topStack).name);
                varName = ((SymbolEntity) topStack).name;
                ArrayList<Integer> new_dimention = new ArrayList<>();
                for (int i = ((SymbolEntity) topStack).dimention.size() - 1; i >=0; i--){
                    new_dimention.add(((SymbolEntity) topStack).dimention.get(i));
                }
                for(int i = dimension.size() - 1; i >= 0; i--){
                    dimString = "";
                    for(int j = 0; j < i; j++){
                        dimString += "[" + new_dimention.get(j) + " x ";
                    }
                    dimString += llvmTypes.get(((SymbolEntity) topStack).arrayElementType);
                    for(int j = 0; j < i; j++){
                        dimString += "]";
                    }
                    llvmLine += "%arrayidx" + llvmRegNum + " = getelementptr inbounds [" + new_dimention.get(i) + " x " + dimString + "], [" +
                            new_dimention.get(i) + " x " + dimString + "]* %" + varName + ", i32 0, i64 " + dimension.get(i) + "\n";
                    varName = "arrayidx" + llvmRegNum;
                    llvmRegNum += 1;
                }
                SymbolEntity resultArr = new SymbolEntity(varName);
                resultArr.type = ((SymbolEntity) topStack).arrayElementType;
                resultArr.isPointer = true;
                semanticStack.push(resultArr);
                addLlvmCode(llvmLine);
                break;


            //*********** function related ***********// 
            case "setFuncArg":
                if(debug){
                    System.out.println("CG: setFuncArg");
                }
                type = (String) semanticStack.pop();
                id = (SymbolEntity) semanticStack.pop();
                id.setType(type);
                String funcName = (String) semanticStack.pop();
                currentSymbolTable.fatherSymbolTable.findSymbol(funcName).funcArgList.add(id);
                semanticStack.push(funcName);
                break;

            case "setFuncType":
                if(debug){
                    System.out.println("CG: setFuncType");
                }
                type = (String) semanticStack.pop();
                funcName = (String) semanticStack.pop();
                currentSymbolTable.fatherSymbolTable.findSymbol(funcName).funcReturnValueType = type;
                String returnTypeLlvm = (String) llvmTypes.get(type);
                ArrayList<SymbolEntity> funcArgList = currentSymbolTable.fatherSymbolTable.findSymbol(funcName).funcArgList;
                llvmLine += "define " + returnTypeLlvm + " @" + funcName + " ( ";
                for (int i = 0; i < funcArgList.size(); i++) {
                    llvmLine += llvmTypes.get(funcArgList.get(i).type) + " " + funcArgList.get(i).name;
                    if( i != funcArgList.size() - 1) {
                        llvmLine += ", ";
                    }
                }
                llvmLine += " ) {\n";
                llvmLine += "entry"+(llvmRegNum) + ": \n";
                addLlvmCode(llvmLine);
                break;
                
            case "endFunc":
                if(debug){
                    System.out.println("CG: endFunc");
                }
                llvmLine += "}";
                addLlvmCode(llvmLine);
                break;
                
            case "return":
                if(debug){
                    System.out.println("CG: return");
                }
                Object returnExpr = semanticStack.pop();
                value = "";
                if (returnExpr instanceof SymbolEntity){
                    value = (String) ((SymbolEntity) returnExpr).value;
                    llvmLine += "ret" + llvmTypes.get(((SymbolEntity) returnExpr).type) + " " + value;
                }
                else {
                    value = (String) returnExpr;
                    llvmLine += "ret i32 " + value;
                }
                addLlvmCode(llvmLine);
                break;
                
            case "pushVoid":
                if(debug){
                    System.out.println("CG: pushVoid");
                }
                semanticStack.push("void");
                break;

            case "funcCall":
                if(debug){
                    System.out.println("CG: funcCall");
                }
                //assumed that function has one and only one argument
                Object arg = semanticStack.pop();
                SymbolEntity functionName = (SymbolEntity) semanticStack.pop();
                if (functionName.name.equals("write")) {
                    if (arg instanceof SymbolEntity) {
                        if (!((SymbolEntity) arg).type.equals("String")) {
                            varName = ((SymbolEntity) arg).name;
                            if(currentSymbolTable.find(((SymbolEntity) arg).name)) {
                                llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(((SymbolEntity) arg).type) + " , " + llvmTypes.get(((SymbolEntity) arg).type) +
                                        "* " + " %" + ((SymbolEntity) arg).name + ", align " + llvmAlign.get(((SymbolEntity) arg).type) + "\n";
                                varName = "t" + llvmRegNum;
                            }
                            else if (((SymbolEntity) arg).isPointer){
                                llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(((SymbolEntity) arg).type) + ", " +
                                        llvmTypes.get(((SymbolEntity) arg).type) + "* %" + ((SymbolEntity) arg).name + ", align " + llvmAlign.get(((SymbolEntity) arg).type) + "\n";
                                varName = "t" + llvmRegNum;
                            }
                            String fuckNum = "";
                            if( ((SymbolEntity) arg).type.equals("char")){
                                fuckNum = ".1";
                            }
                            else if (((SymbolEntity) arg).type.equals("real")){
                                fuckNum = ".3";
                                llvmLine += "%conv" + llvmRegNum + " = fpext float %" + varName + " to double \n";
                                llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @printf(i8* getelementptr inbounds " +
                                        "([3 x i8], [3 x i8]* @.str.fuck" + fuckNum + ", i32 0, i32 0), double %conv" + llvmRegNum + ") \n";

                            }
                            if (!((SymbolEntity) arg).type.equals("real")) {
                                llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @printf(i8* getelementptr inbounds " +
                                        "([3 x i8], [3 x i8]* @.str.fuck" + fuckNum + ", i32 0, i32 0), " + llvmTypes.get(((SymbolEntity) arg).type) + " %" + varName + ") \n";
                            }
                            llvmRegNum += 1;
                            addLlvmCode(llvmLine);
                        } else {
                            String v = (String) ((SymbolEntity) arg).value;
                            String line = "@.str." + llvmRegNum + " = private unnamed_addr constant [" + (v.length() + 1) + " x i8] c\"" + ((SymbolEntity) arg).value + "\\00\", align 1";
                            llvmCode.add(0, line);
                            llvmRegNum += 1;
                            llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @printf(i8* getelementptr inbounds " +
                                    "([3 x i8], [3 x i8]* @.str.fuck.2, i32 0, i32 0), i8* getelementptr inbounds ([" + (v.length() + 1) +
                                    "x i8], [" + (v.length() + 1) + "x i8]* @.str." + (llvmRegNum - 1) + ", i32 0, i32 0))" + "\n";
                            llvmRegNum += 1;
                            addLlvmCode(llvmLine);
                        }

                    } else {
                        String v = (String) arg;
                        String line = "@.str." + llvmRegNum + " = private unnamed_addr constant [" + (v.length() + 1) + " x i8] c\"" + v + "\\00\", align 1";
                        llvmCode.add(0, line);
                        llvmRegNum += 1;
                        llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @printf(i8* getelementptr inbounds " +
                                "([3 x i8], [3 x i8]* @.str.fuck.2, i32 0, i32 0), i8* getelementptr inbounds ([" + (v.length() + 1) +
                                "x i8], [" + (v.length() + 1) + "x i8]* @.str." + (llvmRegNum - 1) + ", i32 0, i32 0))" + "\n";
                        llvmRegNum += 1;
                        addLlvmCode(llvmLine);
                    }
                }
                else if (functionName.name.equals("read")) {
                    if (arg instanceof SymbolEntity) {
                        if (!((SymbolEntity) arg).type.equals("string")){
                            String fuckNum = "";
                            if( ((SymbolEntity) arg).type.equals("char")){
                                fuckNum = ".1";
                            }
                            else if( ((SymbolEntity) arg).type.equals("real")){
                                fuckNum = ".3";
                            }
                            llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @scanf(i8* getelementptr inbounds " +
                                    "([3 x i8], [3 x i8]* @.str.fuck" + fuckNum + ", i32 0, i32 0), " + llvmTypes.get(((SymbolEntity) arg).type) +"* %" + ((SymbolEntity) arg).name +") \n";
                        }
                        else {
                            String fuckNum = ".2";
                            if( ((SymbolEntity) arg).type.equals("char")){
                                fuckNum = ".1";
                            }
                            llvmLine += "%call" + llvmRegNum + " = " + "call i32 (i8*, ...) @scanf(i8* getelementptr inbounds " +
                                    "([3 x i8], [3 x i8]* @.str.fuck" + fuckNum + ", i32 0, i32 0)," + llvmTypes.get(((SymbolEntity) arg).type) +"* %" + ((SymbolEntity) arg).name + ") \n";
                        }
                        llvmRegNum += 1;
                        addLlvmCode(llvmLine);

                    }
                }
                else if (functionName.name.equals("strlen")) {
                    if (arg instanceof SymbolEntity) {
                        llvmLine += "%arraydecay" + llvmRegNum + " = " + "getelementptr inbounds " + "[" + (((String) (((SymbolEntity) arg).value)).length() + 1) + " x i8], [" + (((String) (((SymbolEntity) arg).value)).length() + 1) + " x i8]* %" + ((SymbolEntity) arg).name + ", i32 0, i32 0\n";
                        llvmLine += "%call" + llvmRegNum + " = call i64 @strlen(i8* %arraydecay" + llvmRegNum + ")\n";
                        llvmLine += "%conv" + llvmRegNum + " = trunc i64 %call" + llvmRegNum + " to i32\n";
                        SymbolEntity convReg = new SymbolEntity("conv" + llvmRegNum);
                        llvmRegNum += 1;
                        convReg.type = "integer";
                        semanticStack.push(convReg);
                    }
                    else {
                        semanticStack.push(((String) arg).length() + "");
                    }
                    addLlvmCode(llvmLine);

                }
                break;

            //start of while
            case "addLabelWhileCond":
                if(debug){
                    System.out.println("CG: addLabelWhileCond");
                }
                whileLabelNum += 10;
                llvmLine += "br label %while.cond" + whileLabelNum  + " \n";
                llvmLine += "while.cond" + whileLabelNum + ":  \n";
                llvmCode.add(whileCondLine, llvmLine);
                llvmLine = "";

                Object booleanExpr = semanticStack.pop();

                String condition = "0";
                if (booleanExpr instanceof SymbolEntity){
                    varName = ((SymbolEntity) booleanExpr).name;

                    if(currentSymbolTable.find(((SymbolEntity) booleanExpr).name)) {
                        llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(((SymbolEntity) booleanExpr).type) + " , " + llvmTypes.get(((SymbolEntity) booleanExpr).type) +
                                "* " + " %" + ((SymbolEntity) booleanExpr).name + ", align " + llvmAlign.get(((SymbolEntity) booleanExpr).type) + "\n";
                        varName = "t" + llvmRegNum;
                    }
                    condition = "%tobool" + llvmRegNum;
                    if (((SymbolEntity) booleanExpr).type.equals("integer") || ((SymbolEntity) booleanExpr).type.equals("long")) {
                        llvmLine += "  %tobool" + llvmRegNum + " = icmp ne i32 %" + varName + ", 0\n";

                    } else if (((SymbolEntity) booleanExpr).type.equals("boolean")) {
                        llvmLine += "%tobool" + llvmRegNum + " = trunc i8 %" + varName + " to i1\n";

                    } else if (((SymbolEntity) booleanExpr).type.equals("real")) {
                        llvmLine += " %tobool" + llvmRegNum + " = fcmp une float %" + varName + ", 0.000000e+00\n";
                    }
                    llvmRegNum += 1;
                } else {
                    String boolExpr = (String) booleanExpr;
                    if (boolExpr.equals("true")){
                        condition = "1";
                    } else if (boolExpr.equals("false")) {

                    } else {
                        try {
                            int be = Integer.parseInt(boolExpr);
                            condition = (be > 0) ? "1" : "0";
                        } catch (Exception e) {
                            try {
                                float be = Float.parseFloat(boolExpr);
                                condition = (be > 0.0) ? "1" : "0";
                            } catch (Exception e2){
                                System.out.println("illegal expression for if");
                                //end program
                            }
                        }
                    }
                }

                llvmLine += "br i1 " + condition + ", label %while.body" + whileLabelNum + ", label %while.end" + whileLabelNum + " \n";
                llvmLine += "while.body" + whileLabelNum + ":  \n";
                addLlvmCode(llvmLine);
                break;
                
            case "jumpToWhileCond":
                if(debug){
                    System.out.println("CG: jumpToWhileCond");
                }
                llvmLine += "br label %while.cond" + whileLabelNum  + " \n";
                addLlvmCode(llvmLine);
                break;
                
            case "pushWhile.condLine":
                if(debug){
                    System.out.println("CG: pushWhile.condLine");
                }
                whileCondLine = llvmCode.size();
                break;
            case "addLabelWhileEnd":
                if(debug){
                    System.out.println("CG: addLabelWhileEnd");
                }
                llvmLine += "while.end" + whileLabelNum + ": ";
                addLlvmCode(llvmLine);
                whileLabelNum -= 10;
                break;
                
            case "chooseToJumpIf":
                if(debug){
                    System.out.println("CG: chooseToJumpIf");
                }
                booleanExpr = semanticStack.pop();
                condition = "0";
                if (booleanExpr instanceof SymbolEntity){

                    varName = ((SymbolEntity) booleanExpr).name;

                    if(currentSymbolTable.find(((SymbolEntity) booleanExpr).name)) {
                        llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(((SymbolEntity) booleanExpr).type) + " , " + llvmTypes.get(((SymbolEntity) booleanExpr).type) +
                                "* " + " %" + ((SymbolEntity) booleanExpr).name + ", align " + llvmAlign.get(((SymbolEntity) booleanExpr).type) + "\n";
                        varName = "t" + llvmRegNum;
                    }
                    condition = "%tobool" + llvmRegNum;
                    if (((SymbolEntity) booleanExpr).type.equals("integer") || ((SymbolEntity) booleanExpr).type.equals("long")) {
                        llvmLine += "  %tobool" + llvmRegNum + " = icmp ne i32 %" + varName + ", 0\n";

                    } else if (((SymbolEntity) booleanExpr).type.equals("boolean")) {
                        llvmLine += "%tobool" + llvmRegNum + " = trunc i8 %" + varName + " to i1\n";

                    } else if (((SymbolEntity) booleanExpr).type.equals("real")) {
                        llvmLine += " %tobool" + llvmRegNum + " = fcmp une float %" + varName + ", 0.000000e+00\n";
                    }
                    llvmRegNum += 1;
                } else {
                    String boolExpr = (String) booleanExpr;
                    if (boolExpr.equals("true")){
                        condition = "1";
                    } else if (boolExpr.equals("false")) {

                    } else {
                        try {
                            int be = Integer.parseInt(boolExpr);
                            condition = (be > 0) ? "1" : "0";
                        } catch (Exception e) {
                            try {
                                float be = Float.parseFloat(boolExpr);
                                condition = (be > 0.0) ? "1" : "0";
                            } catch (Exception e2){
                                System.out.println("illegal expression for if");
                                //end program
                            }
                        }
                    }
                }
                ifLableNum += 10;
                llvmLine += "br i1 " + condition + ", label %if.then" + ifLableNum + ", label %if.else" + ifLableNum + " \n";
                llvmLine += "if.then" + ifLableNum + ":  \n";
                addLlvmCode(llvmLine);
                break;

            case "jumpToEndIf":
                if(debug){
                    System.out.println("CG: jumpToEndif");
                }
                llvmLine += "br label %if.end" + ifLableNum + " \n";
                addLlvmCode(llvmLine);
                break;

            case "addLabelElse":
                if(debug){
                    System.out.println("CG: addLabelElse");
                }
                llvmLine += "if.else" + ifLableNum + ":  \n";
                addLlvmCode(llvmLine);
                break;
                
            case "addLabelEndIf":
                if(debug){
                    System.out.println("CG: addLabelEndif");
                }
                llvmLine += "if.end" + ifLableNum + ":  \n";
                addLlvmCode(llvmLine);
                ifLableNum -= 10;
                break;

            case "addBr":
                if(debug){
                    System.out.println("CG: addBr");
                }
                llvmLine += "br label %if.end" + ifLableNum + " \n";
                addLlvmCode(llvmLine);
                break;

            case "cmp_eq":
            case "cmp_g":
            case "cmp_l":
            case "cmp_geq":
            case "cmp_leq":
            case "cmp_neq":
                if(debug){
                    System.out.println("CG: " + sem);
                }
                Object second = semanticStack.pop();
                Object first = semanticStack.pop();

                String varName_1 = "%t" + llvmRegNum;
                llvmRegNum += 1;
                String varName_2 = "%t" + llvmRegNum;
                llvmRegNum += 1;

                int flag = 0;


                if (first instanceof SymbolEntity){

                    if(currentSymbolTable.find(((SymbolEntity) first).name)) {
                        llvmLine += "" + varName_1 + " = load " + llvmTypes.get(((SymbolEntity) first).type) + " , " + llvmTypes.get(((SymbolEntity) first).type) +
                                "* " + " %" + ((SymbolEntity) first).name + ", align " + llvmAlign.get(((SymbolEntity) first).type) + "\n";
                    } else {
                        varName_1 = ((SymbolEntity) first).name;
                    }
                    if (((SymbolEntity) first).type.equals("boolean") || ((SymbolEntity) first).type.equals("char")) {
                        llvmLine += "%conv" + llvmRegNum + " = sext i8 " + varName_1 + " to i32 \n";
                        varName_1 = "%conv" + llvmRegNum;
                        llvmRegNum += 1;
                    }
                    if (((SymbolEntity) first).type.equals("real")) {
                        flag = 1;
                    }
                } else {
                    varName_1 = (String) first;
                    if (varName_1.equals("true")){
                        varName_1 = "1";
                    } else if (varName_1.equals("false")) {
                        varName_1 = "0";
                    }
                }
                if (second instanceof SymbolEntity){

                    if(currentSymbolTable.find(((SymbolEntity) second).name)) {
                        llvmLine += "" + varName_2 + " = load " + llvmTypes.get(((SymbolEntity) second).type) + " , " + llvmTypes.get(((SymbolEntity) second).type) +
                                "* " + " %" + ((SymbolEntity) second).name + ", align " + llvmAlign.get(((SymbolEntity) second).type) + "\n";
                    } else {
                        varName_2 = "%" + ((SymbolEntity) second).name;
                    }
                    if (((SymbolEntity) second).type.equals("boolean") || ((SymbolEntity) second).type.equals("char")) {
                        llvmLine += "%conv" + llvmRegNum + " = sext i8 " + varName_1 + " to i32 \n";
                        varName_2 = "%conv" + llvmRegNum;
                        llvmRegNum += 1;
                    }
                    if (((SymbolEntity) second).type.equals("real")) {
                        flag = 1;
                    }
                } else {
                    varName_2 = (String) second;
                    if (varName_2.equals("true")){
                        varName_2 = "1";
                    } else if (varName_2.equals("false")) {
                        varName_2 = "0";
                    }
                }

                String type_1 = "icmp eq i32";
                if(flag == 0) {
                    if (sem.equals("cmp_eq")) {
                        type_1 = "icmp eq i32";
                    } else if (sem.equals("cmp_geq")) {
                        type_1 = "icmp sge i32";
                    } else if (sem.equals("cmp_leq")) {
                        type_1 = "icmp sle i32";
                    } else if (sem.equals("cmp_neq")) {
                        type_1 = "icmp ne i32";
                    } else if (sem.equals("cmp_l")) {
                        type_1 = "icmp slt i32";
                    } else if (sem.equals("cmp_g")) {
                        type_1 = "icmp sgt i32";
                    }
                }
                else {
                    if (sem.equals("cmp_eq")) {
                        type_1 = "fcmp oeq float";
                    } else if (sem.equals("cmp_geq")) {
                        type_1 = "fcmp oge float";
                    } else if (sem.equals("cmp_leq")) {
                        type_1 = "fcmp ole float";
                    } else if (sem.equals("cmp_neq")) {
                        type_1 = "fcmp une float";
                    } else if (sem.equals("cmp_l")) {
                        type_1 = "fcmp olt float";
                    } else if (sem.equals("cmp_g")) {
                        type_1 = "fcmp ogt float";
                    }
                }

                llvmLine += "%cmp" + llvmRegNum + " = " + type_1 + " " + varName_1 + ", " + varName_2 + " \n";
                llvmRegNum += 1;
                llvmLine += "%conv" + llvmRegNum + " = zext i1 %cmp" + (llvmRegNum - 1) + " to i32";
                llvmRegNum += 1;
                addLlvmCode(llvmLine);
                SymbolEntity resultSymbol = new SymbolEntity("conv" + (llvmRegNum - 1) );
                resultSymbol.type = "integer";
                semanticStack.push(resultSymbol);

                break;

            case "add":
            case "sub":
            case "mult":
            case "div":
            case "mod":
                if(debug){
                    System.out.println("CG: " + sem);
                }
                second = semanticStack.pop();
                first = semanticStack.pop();

                String firstVar = new String();
                String secondVar = new String();

                String first_type = "";
                String second_type = "";

                SymbolEntity sym = null;
                String result_type = "integer";

                if (first instanceof SymbolEntity){
                    SymbolEntity firstSymbol = (SymbolEntity) first;
                    firstVar = "%" + firstSymbol.name;
                    first_type = ((SymbolEntity) first).type;
                    if (currentSymbolTable.find(firstSymbol.name)) {
                        llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                                llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name + ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                        firstVar = "%t" + llvmRegNum;
                        llvmRegNum += 1;
                    }
                    sym = (SymbolEntity) first;
                }
                else {
                    firstVar =  (String) first;
                    if (!(first.equals("true") || first.equals("false"))) {
                        try {
                            Integer.parseInt(firstVar);
                            first_type = "integer";
                        } catch (Exception e3) {
                            try {
                                Float.parseFloat(firstVar);
                                first_type = "real";
                            } catch (Exception e4) {
                                first_type = "char";
                            }
                        }
                    } else if ((first.equals("true") || first.equals("false"))) {
                        boolean f = false;
                        if (first.equals("true")) {
                            f = true;
                        }
                        int fi = f ? 1 : 0;
                        firstVar = fi + "";
                        first_type = "boolean";
                    } else {
                        System.out.println("Illegal Type of operands." + first.getClass());
                    }
                }
                if (second instanceof SymbolEntity){
                    SymbolEntity secondSymbol = (SymbolEntity) second;
                    secondVar = "%" + secondSymbol.name;
                    if(currentSymbolTable.find(secondSymbol.name)) {
                        llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(secondSymbol.type) + ", " +
                                llvmTypes.get(secondSymbol.type) + "*" + " %" + secondSymbol.name + ", " + "align " + llvmAlign.get(secondSymbol.type) + "\n";
                        secondVar = "%t" + llvmRegNum;
                        llvmRegNum += 1;
                    }
                    if (sym == null || !sym.type.equals("real")) {
                        sym = (SymbolEntity) second;
                    }
                }
                else {
                    secondVar = (String) second;
                    if (!(second.equals("true") || second.equals("false"))) {
                        try {
                            Integer.parseInt(secondVar);
                            second_type = "integer";
                        } catch (Exception e3) {
                            try {
                                Float.parseFloat(secondVar);
                                second_type = "real";
                            } catch (Exception e4) {
                                second_type = "char";
                            }
                        }
                    } else if ((second.equals("true") || second.equals("false"))) {
                        boolean s = false;
                        if (second.equals("true")) {
                            s = true;
                        }
                        int si = s ? 1 : 0;
                        secondVar = si + "";
                        second_type = "boolean";
                    } else {
                        System.out.println("Illegal Type of operands." + second.getClass());
                    }
                }

                addLlvmCode(llvmLine);
                llvmLine = "";

                if (sym != null) {
                    if (sym.type.equals("real")){
                        if (first instanceof SymbolEntity) {
                            firstVar = cast(first_type, "real", firstVar);
                            firstVar = "%" + firstVar;
                        }
                        if (second instanceof SymbolEntity) {
                            secondVar = cast(second_type, "real", secondVar);
                            secondVar = "%" + secondVar;
                        }
                        result_type = "real";
                    } else {
                        if (first instanceof SymbolEntity) {
                            firstVar = cast(first_type, "integer", firstVar);
                            firstVar = "%" + firstVar;
                        }
                        if (second instanceof SymbolEntity) {
                            secondVar = cast(second_type, "integer", secondVar);
                            secondVar = "%" + secondVar;
                        }
                    }
                }
                
                if (!(first instanceof SymbolEntity)  && !(second instanceof SymbolEntity)){
                    int intResult = 0;
                    if(sem.equals("add")){
                        intResult = Integer.parseInt(firstVar) + Integer.parseInt(secondVar);
                    }
                    else if (sem.equals("sub")){
                        intResult = Integer.parseInt(firstVar) - Integer.parseInt(secondVar);
                    }
                    else if (sem.equals("mult")){
                        intResult = Integer.parseInt(firstVar) * Integer.parseInt(secondVar);
                    }
                    else if (sem.equals("div")){
                        intResult = Integer.parseInt(firstVar) / Integer.parseInt(secondVar);
                    }
                    else if (sem.equals("mod")){
                        intResult = Integer.parseInt(firstVar) % Integer.parseInt(secondVar);
                    }
                    semanticStack.push("" + intResult);
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                }
                else {
                    SymbolEntity result = new SymbolEntity("tempResult");

                    if(sem.equals("add")){
                        String typo = "add nsw i32";
                        if (result_type.equals("real")){
                            typo = "fadd float";
                        }
                        llvmLine += "%t" + llvmRegNum + " = " + typo +
                                " " + firstVar + ", " +  " " + secondVar + "\n";
                    }
                    else if(sem.equals("sub")){
                        String typo = "sub nsw i32";
                        if (result_type.equals("real")){
                            typo = "fsub float";
                        }
                        llvmLine += "%t" + llvmRegNum + " = " + typo +
                                " " + firstVar + ", " +  " " + secondVar + "\n";
                    }
                    else if(sem.equals("mult")){
                        String typo = "mul nsw i32";
                        if (result_type.equals("real")){
                            typo = "fmul float";
                        }
                        llvmLine += "%t" + llvmRegNum + " = " + typo +
                                " " + firstVar + ", " +  " " + secondVar + "\n";
                    }
                    else if(sem.equals("div")){
                        String typo = "sdiv i32";
                        if (result_type.equals("real")){
                            typo = "fdiv float";
                        }
                        llvmLine += "%t" + llvmRegNum + " =  " + typo +
                                " " + firstVar + ", " +  " " + secondVar + "\n";
                    }
                    else if(sem.equals("mod")){

                        if (result_type.equals("real")){
                            System.out.println("Invalid operand for real and real");
                        }
                        llvmLine += "%t" + llvmRegNum + " = srem " + llvmTypes.get(sym.type) +
                                " " + firstVar + ", " +  " " + secondVar + "\n";
                    }
                    result.name = "t" + llvmRegNum;
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                    result.type = result_type;
                    semanticStack.push(result);
                }
                break;

            case "eadd":
                if(debug){
                    System.out.println("CG: " + sem);
                }
                second = semanticStack.pop();
                first = semanticStack.pop();

                firstVar = new String();
                secondVar = new String();
                if (first instanceof SymbolEntity){
                    SymbolEntity firstSymbol = (SymbolEntity) first;
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                            llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name +  ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                    firstVar += "%t" + llvmRegNum;
                    llvmRegNum += 1;
                }
                else {
                    firstVar = Integer.parseInt((String) first) + "";
                }

                if (second instanceof SymbolEntity){
                    SymbolEntity secondSymbol = (SymbolEntity) second;
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(secondSymbol.type) + ", " +
                            llvmTypes.get(secondSymbol.type) + "*" + " %" + secondSymbol.name +  ", " + "align " + llvmAlign.get(secondSymbol.type) + "\n";
                    secondVar += "%t" + llvmRegNum;
                    llvmRegNum += 1;
                }
                else {
                    secondVar = Integer.parseInt((String) second) + "";
                }

                if (!(first instanceof SymbolEntity)  && !(second instanceof SymbolEntity)){
                    int intResult = 0;
                    intResult = Integer.parseInt(firstVar) ^ Integer.parseInt(secondVar);
                    semanticStack.push("" + intResult);
                }
                else {
                    SymbolEntity result = new SymbolEntity("tempResult");

                    llvmLine += "%t" + llvmRegNum + " = xor " + llvmTypes.get("integer") +
                            " " + firstVar + ", " +  " " + secondVar + "\n";

                    result.name = "t" + llvmRegNum;
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                    result.type = "integer";
                    semanticStack.push(result);
                }

                break;


            case "sminus":
            case "snot":
                if(debug){
                    System.out.println("CG: " + sem);
                }
                first = semanticStack.pop();
                firstVar = new String();
                if (first instanceof SymbolEntity){
                    SymbolEntity firstSymbol = (SymbolEntity) first;
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                            llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name +  ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                    llvmRegNum += 1;
                    if (sem.equals("sminus")) {
                        llvmLine += "%t" + llvmRegNum + " = sub nsw " + llvmTypes.get("integer") + " 0, " + " %t" + (llvmRegNum - 1) + "\n";
                    }
                    else if (sem.equals("snot")) {
                        llvmLine += "%t" + llvmRegNum + " = xor " + llvmTypes.get("integer") + " %t" + (llvmRegNum - 1) + ", -1 \n";
                    }
                    addLlvmCode(llvmLine);
                    sym = new SymbolEntity("t" + llvmRegNum);
                    sym.type = "integer";
                    semanticStack.push(sym);
                    llvmRegNum += 1;

                }
                else {
                    firstVar = first + "";
                    int result = 0 - Integer.parseInt(firstVar);
                    semanticStack.push(result + "");

                }

                break;

            case "bor":
            case "band":
                if(debug){
                    System.out.println("CG: bor/band");
                }
                first = semanticStack.pop();
                second = semanticStack.pop();
                if (!(first instanceof SymbolEntity) && !(second instanceof SymbolEntity)){
                    if (!(first.equals("true") || first.equals("false"))) {
                        first = Integer.parseInt((String) first);
                        second = Integer.parseInt((String) second);
                        int intResult;
                        if(sem.equals("bor")){intResult = ((int) first | (int) second);}
                        else{intResult = ((int) first & (int) second);}
                        semanticStack.push(""+intResult);
                    }
                    else if (first instanceof Long) {
                        long longResult;
                        if(sem.equals("bor")){longResult = ((long) first | (long) second);}
                        else{longResult = ((long) first & (long) second);}
                        semanticStack.push(""+longResult);
                    }
                    else if ((first.equals("true") || first.equals("false"))) {
                        boolean f = false;
                        boolean s = false;
                        if( first.equals("true")){f = true;}
                        if( second.equals("true")){s = true;}
                        boolean boolResult;
                        if(sem.equals("bor")){boolResult = ( f | s);}
                        else{boolResult = ( f & s);}
                        int boolIntRes = boolResult ? 1 : 0;
                        semanticStack.push(""+boolIntRes);
                    }
                    else {
                        System.out.println("Illegal Type of operands." + first.getClass());
                    }
                }
                else  {
                    if (first instanceof SymbolEntity) {
                        firstVar = "%" + ((SymbolEntity) first).name;
                        SymbolEntity firstSymbol = (SymbolEntity) first;
                        if (currentSymbolTable.find(firstSymbol.name)) {
                            llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                                    llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name + ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                            firstVar = "%t" + llvmRegNum;
                        }
                        if (firstSymbol.type.equals("boolean")) {
                            llvmLine += "%tobool1 " + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                            llvmLine += "%tconv1" + " = " + "zext " + "i1" + " %tobool1 " + "to i32\n";
                            firstVar = "%tconv1";
                        }
                        llvmRegNum += 1;
                    } else {
                        firstVar = (String) first;
                        if (firstVar.equals("true")){
                            firstVar = "1";
                        } else if (firstVar.equals("false")) {
                            firstVar = "0";
                        }
                    }

                    if (second instanceof SymbolEntity) {
                        secondVar = "%" + ((SymbolEntity) second).name;
                        SymbolEntity secondSymbol = (SymbolEntity) second;
                        if (currentSymbolTable.find(secondSymbol.name)) {
                            llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(secondSymbol.type) + ", " + llvmTypes.get(secondSymbol.type) +
                                    "*" + " %" + secondSymbol.name + ", " + "align " + llvmAlign.get(secondSymbol.type) + "\n";
                            secondVar = "%t" + llvmRegNum;
                        }
                        if (secondSymbol.type.equals("boolean")) {
                            llvmLine += "%tobool2 " + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                            llvmLine += "%tconv2" + " = " + "zext " + "i1" + " %tobool2 " + "to i32\n";
                            secondVar = "%tconv2";
                        }
                        llvmRegNum += 1;
                    } else {
                        secondVar = (String) second;
                        if (secondVar.equals("true")){
                            secondVar = "1";
                        } else if (secondVar.equals("false")) {
                            secondVar = "0";
                        }
                    }

                    SymbolEntity result = new SymbolEntity("tempResult");
                    if(sem.equals("bor")){llvmLine += "%t" + llvmRegNum + " = or " + llvmTypes.get("integer") +
                            " " + firstVar + ", " +  "" + secondVar + "\n";}
                    else{llvmLine += "%t" + llvmRegNum + " = and " + llvmTypes.get("integer") +
                            " " + firstVar + ", " +  "" + secondVar + "\n";}
                    result.name = "t" + llvmRegNum;
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                    result.type = "integer";
                    semanticStack.push(result);
                }
                break;
                
            case "or":
                if(debug){
                    System.out.println("CG: or");
                }
                first = semanticStack.pop();
                second = semanticStack.pop();
                if (!(first instanceof SymbolEntity & second instanceof SymbolEntity)){
                    boolean result;
                    if(!(first.equals("true") || first.equals("false"))){
                        first = Integer.parseInt((String) first);
                        second = Integer.parseInt((String) second);
                        result =  ((int)first > 0 ? true : false) ||  ((int)second > 0 ? true : false);
                    }
                    else {
                        boolean f = false;
                        boolean s = false;
                        if( first.equals("true")){f = true;}
                        if( second.equals("true")){s = true;}
                        result = (f || s);
                    }
                    int fr = result ? 1 : 0;
                    semanticStack.push(""+fr);

                }
                else {
                    SymbolEntity firstSymbol = (SymbolEntity) first;
                    SymbolEntity secondSymbol = (SymbolEntity) second;
                    SymbolEntity result = new SymbolEntity("tempResult");

                    firstVar = new String();
                    secondVar = new String();


                    String[] legalTypes = new String[]{"integer", "boolean", "long"};
                    List<String> list = Arrays.asList(legalTypes);
                    if (!(list.contains(firstSymbol.type) & list.contains(secondSymbol.type))) {
                        System.out.println("Illegal Type of operands.");
                        //end the program
                    }
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                            llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name +  ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                    firstVar += llvmRegNum;
                    if( firstSymbol.type.equals("boolean")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                        firstVar = "tobool" + llvmRegNum;
                    }
                    else if(firstSymbol.type.equals("integer")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "icmp ne " + llvmTypes.get("integer") + " %t" + llvmRegNum + ", 0\n" ;
                        firstVar = "tobool" + llvmRegNum;
                    }
                    llvmLine += "br i1 %tobool"+llvmRegNum + ", label %lor.end"+llvmRegNum + ", label %lor.rhs"+llvmRegNum + "\n";
                    llvmRegNum += 1;

                    llvmLine += "lor.rhs"+(llvmRegNum-1) + ": \n";
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(secondSymbol.type) + ", " + llvmTypes.get(secondSymbol.type) +
                            "*" + " %" + secondSymbol.name +  ", " + "align " + llvmAlign.get(secondSymbol.type) + "\n";
                    secondVar += llvmRegNum;
                    if( secondSymbol.type.equals("boolean")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                        secondVar = "tobool" + llvmRegNum;
                    }
                    else if(secondSymbol.type.equals("integer")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "icmp ne " + llvmTypes.get("integer") + " %t" + llvmRegNum + ", 0\n" ;
                        firstVar = "tobool" + llvmRegNum;
                    }
                    llvmLine += "br label %lor.end"+(llvmRegNum-1) + "\n";

                    llvmLine += "lor.end"+(llvmRegNum-1) + ":\n";
                    llvmRegNum += 1;
                    llvmLine += "%t"+llvmRegNum + " = phi i1 [ true, %entry"+(llvmRegNum - 2) + " ], [ %tobool"+(llvmRegNum-1) + ", %lor.rhs"+(llvmRegNum-2) + " ]\n";
                    llvmRegNum += 1;
                    llvmLine += "%lor.ext"+llvmRegNum + " = zext i1 %t"+(llvmRegNum-1) + " to " + llvmTypes.get("integer") + "\n";
                    result.name = "lor.ext"+llvmRegNum;
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                    result.type = "integer";
                    semanticStack.push(result);
                }
                break;

            case "and":
                if(debug){
                    System.out.println("CG: and");
                }
                first = semanticStack.pop();
                second = semanticStack.pop();
                if (!(first instanceof SymbolEntity & second instanceof SymbolEntity)){
                    boolean result;
                    if(!(first.equals("true") || first.equals("false"))){
                        first = Integer.parseInt((String) first);
                        second = Integer.parseInt((String) second);
                        result =  ((int)first > 0 ? true : false) &&  ((int)second > 0 ? true : false);
                    }
                    else {
                        boolean f = false;
                        boolean s = false;
                        if( first.equals("true")){f = true;}
                        if( second.equals("true")){s = true;}
                        result = (f && s);
                    }
                    int fr = result ? 1 : 0;
                    semanticStack.push(""+fr);
                }
                else {
                    SymbolEntity firstSymbol = (SymbolEntity) first;
                    SymbolEntity secondSymbol = (SymbolEntity) second;
                    SymbolEntity result = new SymbolEntity("tempResult");

                    firstVar = new String();
                    secondVar = new String();


                    String[] legalTypes = new String[]{"integer", "boolean", "long"};
                    List<String> list = Arrays.asList(legalTypes);
                    if (!(list.contains(firstSymbol.type) & list.contains(secondSymbol.type))) {
                        System.out.println("Illegal Type of operands.");
                        //end the program
                    }
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(firstSymbol.type) + ", " +
                            llvmTypes.get(firstSymbol.type) + "*" + " %" + firstSymbol.name +  ", " + "align " + llvmAlign.get(firstSymbol.type) + "\n";
                    firstVar += llvmRegNum;
                    if( firstSymbol.type.equals("boolean")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                        firstVar = "tobool" + llvmRegNum;
                    }
                    else if(firstSymbol.type.equals("integer")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "icmp ne " + llvmTypes.get("integer") + " %t" + llvmRegNum + ", 0\n" ;
                        firstVar = "tobool" + llvmRegNum;
                    }
                    llvmLine += "br i1 %tobool"+llvmRegNum + ", label %land.rhs"+llvmRegNum + ", label %land.end"+llvmRegNum + "\n";
                    llvmRegNum += 1;

                    llvmLine += "land.rhs"+(llvmRegNum-1) + ": \n";
                    llvmLine += "%t" + llvmRegNum + " = load " + llvmTypes.get(secondSymbol.type) + ", " + llvmTypes.get(secondSymbol.type) +
                            "*" + " %" + secondSymbol.name +  ", " + "align " + llvmAlign.get(secondSymbol.type) + "\n";
                    secondVar += llvmRegNum;
                    if( secondSymbol.type.equals("boolean")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "trunc " + llvmTypes.get("boolean") + " %t" + llvmRegNum + " to " + "i1\n";
                        secondVar = "tobool" + llvmRegNum;
                    }
                    else if(secondSymbol.type.equals("integer")){
                        llvmLine += "%tobool" + llvmRegNum + " = " + "icmp ne " + llvmTypes.get("integer") + " %t" + llvmRegNum + ", 0\n" ;
                        firstVar = "tobool" + llvmRegNum;
                    }
                    llvmLine += "br label %land.end"+(llvmRegNum-1) + "\n";

                    llvmLine += "land.end"+(llvmRegNum-1) + ":\n";
                    llvmRegNum += 1;
                    llvmLine += "%t"+llvmRegNum + " = phi i1 [ false, %entry"+(llvmRegNum - 2) + " ], [ %tobool"+(llvmRegNum-1) + ", %land.rhs"+(llvmRegNum-2) + " ]\n";
                    llvmRegNum += 1;
                    llvmLine += "%land.ext"+llvmRegNum + " = zext i1 %t"+(llvmRegNum-1) + " to " + llvmTypes.get("integer") + "\n";
                    result.name = "land.ext"+llvmRegNum;
                    llvmRegNum += 1;
                    addLlvmCode(llvmLine);
                    result.type = "integer";
                    semanticStack.push(result);
                }
                break;

            default:
                System.out.println("Illegal semantic function: " + sem);
        }
    }
}
