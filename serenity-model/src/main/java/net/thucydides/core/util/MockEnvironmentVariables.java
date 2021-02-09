package net.thucydides.core.util;

import net.serenitybdd.core.collect.NewMap;
import net.serenitybdd.core.environment.ConfiguredEnvironment;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MockEnvironmentVariables implements EnvironmentVariables {

    private Properties properties = new Properties();
    private Map<String, String> values = new HashMap<>();

    public MockEnvironmentVariables() {
        this.properties.setProperty("user.home", System.getProperty("user.home"));
        this.properties.setProperty("feature.file.encoding", "UTF-8");
        if (localEnvironment().getProperty("phantomjs.binary.path") != null) {
            this.properties.setProperty("phantomjs.binary.path", localEnvironment().getProperty("phantomjs.binary.path"));
        }
        if (localEnvironment().getProperty("webdriver.chrome.driver") != null) {
            this.properties.setProperty("webdriver.chrome.driver", localEnvironment().getProperty("webdriver.chrome.driver"));
        }
    }

    private EnvironmentVariables localEnvironment() {
        return ConfiguredEnvironment.getEnvironmentVariables();
    }

    protected MockEnvironmentVariables(Properties properties) {
        this.properties = PropertiesUtil.copyOf(properties);
    }

    protected MockEnvironmentVariables(Properties properties, Map<String, String> values) {
        this.properties = PropertiesUtil.copyOf(properties);
        this.values = NewMap.copyOf(values);
    }

    public static EnvironmentVariables fromSystemEnvironment() {
        return new MockEnvironmentVariables(SystemEnvironmentVariables.createEnvironmentVariables().getProperties());
    }

    public boolean propertySetIsEmpty() {
        return properties.isEmpty();
    }

    public String getValue(String name) {
        return values.get(name);
    }


    public String getValue(Enum<?> property) {
        return getValue(property.toString());
    }

    public String getValue(String name, String defaultValue) {
        return values.get(name) == null ? defaultValue : values.get(name);
    }

    public String getValue(Enum<?> property, String defaultValue) {
        return getValue(property.toString(), defaultValue);
    }

    public Integer getPropertyAsInteger(String name, Integer defaultValue) {
        String value = (String) properties.get(name);
        if (StringUtils.isNumeric(value)) {
            return Integer.parseInt(properties.getProperty(name));
        } else {
            return defaultValue;
        }
    }


    public Integer getPropertyAsInteger(Enum<?> property, Integer defaultValue) {
        return getPropertyAsInteger(property.toString(), defaultValue);
    }

    public Boolean getPropertyAsBoolean(String name, boolean defaultValue) {
        if (properties.getProperty(name) == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(properties.getProperty(name, "false"));
        }
    }


    public Boolean getPropertyAsBoolean(Enum<?> property, boolean defaultValue) {
        return getPropertyAsBoolean(property.toString(), defaultValue);
    }

    public String getProperty(String name) {
        if (name != null) {
            return properties.getProperty(name);
        } else {
            return null;
        }
    }

    @Override
    public Optional<String> optionalProperty(String name) {
        return Optional.ofNullable(getProperty(name));
    }


    public String getProperty(Enum<?> property) {
        return getProperty(property.toString());
    }

    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }


    public String getProperty(Enum<?> property, String defaultValue) {
        return getProperty(property.toString(), defaultValue);
    }

    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }

    public void setProperties(Map<String, String> newProperties) {
        properties.putAll(newProperties);
    }


    public void clearProperty(String name) {
        properties.remove(name);
    }

    public EnvironmentVariables copy() {
        return new MockEnvironmentVariables(properties, values);
    }

    @Override
    public List<String> getKeys() {
        return properties.keySet().stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public Properties getProperties() {
        return new Properties(properties);
    }

    @Override
    public Properties getPropertiesWithPrefix(String prefix) {
        return new SystemEnvironmentVariables(properties, values).getPropertiesWithPrefix(prefix);
    }

    @Override
    public boolean aValueIsDefinedFor(Enum<?> property) {
        return properties.contains(property.toString());
    }

    @Override
    public boolean aValueIsDefinedFor(String property) {
        return properties.contains(property);
    }

    @Override
    public String injectSystemPropertiesInto(String value) {
        return value;
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> environmentValues = new HashMap<>();

        values.keySet().forEach(
                key -> environmentValues.put(key, values.get(key))
        );
        properties.stringPropertyNames().forEach(
                key -> environmentValues.put(key, properties.getProperty(key))
        );
        return environmentValues;
    }

    @Override
    public Map<String, String> simpleSystemPropertiesAsMap() {
        return new HashMap<>();
    }

    public void setValue(String name, String value) {
        values.put(name, value);
    }

}
