
package semantic.ast.expression.constant;

import semantic.ast.expression.Expression;

public class StringConst implements Expression {
    private String value;

    public StringConst(String value) {
        this.value = value;
    }

    @Override
    public void codegen() {
        System.out.println("ldc " + value);
    }
}
