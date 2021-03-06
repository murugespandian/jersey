/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.tests.integration.jersey2612;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;

@Singleton
public class OptionalParamConverterProvider implements ParamConverterProvider {

    private final ServiceLocator locator;

    @Inject
    public OptionalParamConverterProvider(final ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        final List<ClassTypePair> ctps = ReflectionHelper.getTypeArgumentAndClass(genericType);
        final ClassTypePair ctp = (ctps.size() == 1) ? ctps.get(0) : null;
        if (ctp == null || ctp.rawClass() == String.class) {
            return new ParamConverter<T>() {
                @Override
                public T fromString(final String value) {
                    return rawType.cast(Optional.fromNullable(value));
                }

                @Override
                public String toString(final T value) throws IllegalArgumentException {
                    return value.toString();
                }
            };
        }
        final Set<ParamConverterProvider> converterProviders = Providers.getProviders(locator, ParamConverterProvider.class);
        for (ParamConverterProvider provider : converterProviders) {
            @SuppressWarnings("unchecked")
            final ParamConverter<?> converter = provider.getConverter(ctp.rawClass(), ctp.type(), annotations);
            if (converter != null) {
                return new ParamConverter<T>() {
                    @Override
                    public T fromString(final String value) {
                        return rawType.cast(Optional.fromNullable(value)
                                                    .transform(new Function<String, Object>() {
                                                        @Override
                                                        public Object apply(final String s) {
                                                            return converter.fromString(value);
                                                        }
                                                    }));
                    }

                    @Override
                    public String toString(final T value) throws IllegalArgumentException {
                        return value.toString();
                    }
                };
            }
        }
        return null;
    }
}