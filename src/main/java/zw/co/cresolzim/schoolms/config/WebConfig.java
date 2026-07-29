package zw.co.cresolzim.schoolms.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import zw.co.cresolzim.schoolms.domain.BaseEntity;

/**
 * Dropdowns post an entity's id as a string. This converts that back into the
 * entity, so a form can bind straight to a field like SchoolClass.classTeacher
 * instead of carrying a parallel teacherId everywhere.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final EntityManager entityManager;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new IdToEntityConverterFactory(entityManager));
    }

    static class IdToEntityConverterFactory implements ConverterFactory<String, BaseEntity> {

        private final EntityManager em;

        IdToEntityConverterFactory(EntityManager em) { this.em = em; }

        @Override
        public <T extends BaseEntity> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> {
                if (source == null || source.isBlank()) return null;
                try {
                    return em.find(targetType, Long.valueOf(source.trim()));
                } catch (NumberFormatException e) {
                    return null;
                }
            };
        }
    }
}
