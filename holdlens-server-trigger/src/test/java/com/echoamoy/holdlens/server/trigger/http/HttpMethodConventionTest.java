package com.echoamoy.holdlens.server.trigger.http;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

public class HttpMethodConventionTest {

    private static final Set<RequestMethod> FORBIDDEN_METHODS =
            Set.of(RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Test
    public void controllersOnlyUseGetAndPostMappings() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (var candidate : scanner.findCandidateComponents("com.echoamoy.holdlens.server.trigger.http")) {
            Class<?> controllerClass = Class.forName(candidate.getBeanClassName());
            for (Method method : controllerClass.getDeclaredMethods()) {
                Assert.assertNull(method.toString(), method.getAnnotation(PutMapping.class));
                Assert.assertNull(method.toString(), method.getAnnotation(PatchMapping.class));
                Assert.assertNull(method.toString(), method.getAnnotation(DeleteMapping.class));
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                if (requestMapping != null) {
                    Assert.assertTrue(method.toString(), Arrays.stream(requestMapping.method())
                            .noneMatch(FORBIDDEN_METHODS::contains));
                }
            }
        }
    }
}
