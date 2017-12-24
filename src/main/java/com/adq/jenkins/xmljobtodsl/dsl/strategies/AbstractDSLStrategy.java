package com.adq.jenkins.xmljobtodsl.dsl.strategies;

import com.adq.jenkins.xmljobtodsl.IDescriptor;
import com.adq.jenkins.xmljobtodsl.PropertyDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class AbstractDSLStrategy implements DSLStrategy {

    private static final String PREFIX_GROOVY = "groovy.";
    private static final String SUFIX_GROOVY_TYPE = ".type";

    private static final String TYPE_CONST = "CONST";
    private static final String TYPE_INNER = "INNER";
    private static final String TYPE_OBJECT = "OBJECT";
    private static final String TYPE_METHOD = "METHOD";
    private static final String TYPE_PARAMETER = "PARAMETER";
    private static final String TYPE_PROPERTY = "PROPERTY";

    private Properties syntaxProperties;
    private Properties translatorProperties;

    private List<DSLStrategy> children = new ArrayList<>();

    protected int tabs = 0;

    public AbstractDSLStrategy(IDescriptor descriptor, int tabs) {
        try {
            initProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initChildren(descriptor);
    }

    private void initProperties() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("syntax.properties");
        syntaxProperties = new Properties();
        syntaxProperties.load(in);

        in = getClass().getClassLoader().getResourceAsStream("translator.properties");
        translatorProperties = new Properties();
        translatorProperties.load(in);
    }

    public DSLStrategy getStrategyByPropertyDescriptorType(PropertyDescriptor propertyDescriptor) {
        String key = String.format("%s%s", PREFIX_GROOVY, propertyDescriptor.getName());
        String property = translatorProperties.getProperty(key);

        if (property == null) {
            return null;
        }

        if (TYPE_INNER.equals(property)) {
            return new DSLInnerStrategy(propertyDescriptor);
        }

        String type = String.format("%s%s", key, SUFIX_GROOVY_TYPE);
        String propertyType = translatorProperties.getProperty(type);

        if (propertyType == null) {
            propertyType = TYPE_METHOD;
        }

        switch (propertyType) {
            case TYPE_CONST:
                return new DSLConstantStrategy(propertyDescriptor, property);
            case TYPE_OBJECT:
                return new DSLObjectStrategy(getTabs(), propertyDescriptor, property);
            case TYPE_PARAMETER:
                return new DSLParameterStrategy(propertyDescriptor);
            case TYPE_PROPERTY:
                return new DSLPropertyStrategy(getTabs(), propertyDescriptor, property);
            default:
                return new DSLMethodStrategy(getTabs(), propertyDescriptor, property);
        }
    }

    public Properties getSyntaxProperties() {
        return syntaxProperties;
    }

    @Override
    public List<DSLStrategy> getChildren() {
        return children;
    }

    @Override
    public void addChild(DSLStrategy strategy) {
        children.add(strategy);
    }

    protected void initChildren(IDescriptor descriptor) {
        if (descriptor.getProperties() == null) {
            return;
        }
        for (PropertyDescriptor propertyDescriptor : descriptor.getProperties()) {
            DSLStrategy strategy = getStrategyByPropertyDescriptorType(propertyDescriptor);
            if (strategy != null) {
                addChild(strategy);
            }
        }
    }

    protected String getChildrenDSL() {
        tabs++;
        StringBuilder dsl = new StringBuilder();
        for (DSLStrategy strategy : getChildren()) {
            dsl.append(getTabsString());
            dsl.append(strategy.toDSL());
        }
        return dsl.toString();
    }

    public String printValueAccordingOfItsType(String value) {
        if (value == null) {
            return "null";
        }
        if (value.equals("true") || value.equals("false")) {
            return value;
        }
        if (value.matches("[0-9.]+")) {
            return value;
        }
        if (value.contains("\n")) {
            return "\"\"\"" + value + "\"\"\"";
        }
        return "\"" + value + "\"";
    }

    @Override
    public int getTabs() {
        return tabs;
    }

    public String getTabsString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < getTabs(); i++) {
            builder.append("\t");
        }
        return builder.toString();
    }
}