package edu.berkeley.nlp.syntax;

import java.util.List;

public class NamedTree<L> extends Tree<L> {
    private static final long serialVersionUID = 8841212195373673057L;
    private final String name;

    public NamedTree(L label, List<Tree<L>> children, String name) {

        super(label, children);
        this.name = name;
    }

    public NamedTree(L label, String name) {

        super(label);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
