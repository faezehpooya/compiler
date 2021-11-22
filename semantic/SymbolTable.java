package semantic;

import semantic.ast.SymbolEntity;

import java.util.ArrayList;


class SymbolTable {
	public ArrayList<SymbolEntity> symbolList = new ArrayList<SymbolEntity>();
	public SymbolTable fatherSymbolTable;

	public void setFatherSymbolTable(SymbolTable symbolTable){
		this.fatherSymbolTable = symbolTable;
	}
	
	public boolean add(SymbolEntity sym){
		for (int i = 0; i < symbolList.size(); i++) {
			if(sym.name.equals(symbolList.get(i).name)){
				return false;
			}
		}
		symbolList.add(sym);
		return true;
	}
	
	public boolean find(String sym){
		for (int i = 0; i < symbolList.size(); i++) {
			if(sym.equals(symbolList.get(i).name)){
				return true;
			}
		}
		return false;
	}
	
	public SymbolEntity findSymbol(String sym){
		for (int i = 0; i < symbolList.size(); i++) {
			if(sym.equals(symbolList.get(i).name)){
				return symbolList.get(i);
			}
		}
		return null;
	}
	
}
