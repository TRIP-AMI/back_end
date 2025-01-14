package TripAmi.backend.app.util.infra;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return String.join(" ", attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return Arrays.stream(dbData.split(" ")).collect(Collectors.toList());
    }
}
