package bank.repository.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

/**
 * SQLite stores dates as TEXT (ISO format: "2024-01-15").
 * This converter makes it transparent to JPA entities.
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : LocalDate.parse(dbData);
    }
}
