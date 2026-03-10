package com.chitchat.server.model;

import com.chitchat.shared.UserPreferences;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserPreferencesConverter implements AttributeConverter<UserPreferences, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(UserPreferences prefs) {
        if (prefs == null) return null;
        try {
            return mapper.writeValueAsString(prefs);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public UserPreferences convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new UserPreferences();
        try {
            return mapper.readValue(json, UserPreferences.class);
        } catch (Exception e) {
            return new UserPreferences();
        }
    }
}
