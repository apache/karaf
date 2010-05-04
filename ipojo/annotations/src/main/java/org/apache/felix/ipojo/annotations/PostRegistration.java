package org.apache.felix.ipojo.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;


/**
 * This annotation declares a post-service-registration method.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Target(ElementType.METHOD)
public @interface PostRegistration {

}
