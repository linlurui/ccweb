package ccait.ccweb.annotation;

import entity.query.annotation.DataSource;
import entity.query.annotation.Tablename;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Component
@Tablename
@Documented
@DataSource
@Scope("prototype")
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    @AliasFor(annotation = Component.class)
    String value() default "";
    String dataSource() default "";
    String table() default "";
}