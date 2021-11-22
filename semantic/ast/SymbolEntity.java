package semantic.ast;

import java.util.ArrayList;

public class SymbolEntity {
    public String name;
    public String type;
    public Object value;
    public String arrayElementType;
    public ArrayList<Integer> dimention = new ArrayList<>();
    public ArrayList<SymbolEntity> funcArgList = new ArrayList<SymbolEntity>();
    public String funcReturnValueType;
    public boolean isPointer = false;

    public SymbolEntity(String name){
        this.name = name;
    }
    public void setType(String type){
        this.type = type;
    }
}
