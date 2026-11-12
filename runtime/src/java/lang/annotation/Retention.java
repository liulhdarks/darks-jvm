package java.lang.annotation;

@Retention(RetentionPolicy.RUNTIME)
public @interface Retention
{
    RetentionPolicy value();
}
