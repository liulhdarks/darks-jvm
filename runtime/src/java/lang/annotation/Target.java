package java.lang.annotation;

@Retention(RetentionPolicy.RUNTIME)
public @interface Target
{
    ElementType[] value();
}
