/*
 * Copyright 2008-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.injection;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.List;

import static griffon.util.GriffonNameUtils.getPropertyName;
import static java.util.Objects.requireNonNull;

/**
 * Code lifted from {@code juzu.impl.inject.spi.spring.SingletonBean} original by Julien Viet
 *
 * @author Andres Almiray
 * @since 2.4.0
 */
public class SingletonBean<T> extends AbstractSpringBean<T> {
    final T instance;

    public SingletonBean(
        @Nonnull Class<T> type,
        @Nonnull T instance,
        @Nonnull List<AutowireCandidateQualifier> qualifiers) {
        super(type, qualifiers);
        this.instance = requireNonNull(instance, "Argument 'instance' must not be null");
    }

    @Override
    public void configure(@Nonnull String name, @Nonnull SpringContext context, @Nonnull DefaultListableBeanFactory factory) {
        AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(getType());
        definition.setScope(getPropertyName(Singleton.class));
        for (AutowireCandidateQualifier qualifier : getQualifiers()) {
            definition.addQualifier(qualifier);
        }
        factory.registerBeanDefinition(name, definition);

        context.getInstances().put(name, instance);
    }
}
