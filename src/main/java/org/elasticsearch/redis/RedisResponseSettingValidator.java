package org.elasticsearch.redis;

import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.collect.ImmutableSet;


public class RedisResponseSettingValidator implements Validator {

    private static final ImmutableSet<String> options = ImmutableSet.of("standard", "json");

    public static final RedisResponseSettingValidator INSTANCE = new RedisResponseSettingValidator();

    @Override
    public String validate(String setting, String value) {
        if (!options.contains(value)) {
            return "Incorrect config value: " + value;
        }
        return null;
    }
}
