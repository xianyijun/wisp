package cn.xianyijun.wisp.filter;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class MessageEvaluationContext implements EvaluationContext {
    private Map<String, String> properties;

    @Override
    public Object get(final String name) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(name);
    }

    @Override
    public Map<String, Object> keyValues() {
        if (properties == null) {
            return null;
        }

        Map<String, Object> copy = new HashMap<>(properties.size(), 1);

        for (String key : properties.keySet()) {
            copy.put(key, properties.get(key));
        }

        return copy;
    }
}