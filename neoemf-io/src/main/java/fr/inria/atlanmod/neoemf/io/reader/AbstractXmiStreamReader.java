/*
 * Copyright (c) 2013-2018 Atlanmod, Inria, LS2N, and IMT Nantes.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 */

package fr.inria.atlanmod.neoemf.io.reader;

import fr.inria.atlanmod.commons.log.Log;
import fr.inria.atlanmod.commons.primitive.Strings;
import fr.inria.atlanmod.neoemf.io.bean.BasicReference;
import fr.inria.atlanmod.neoemf.io.processor.Processor;
import fr.inria.atlanmod.neoemf.io.util.XmiConstants;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.nonNull;

/**
 * An {@link AbstractStreamReader} that processes the raw structure of XMI files.
 */
@ParametersAreNonnullByDefault
public abstract class AbstractXmiStreamReader extends AbstractStreamReader {

    /**
     * Constructs a new {@code AbstractXmiStreamReader} with the given {@code processor}.
     *
     * @param processor the processor to notify
     */
    public AbstractXmiStreamReader(Processor processor) {
        super(processor);
    }

    @Override
    protected final boolean isSpecialAttribute(@Nullable String prefix, String name, String value) {
        if (Objects.equals(XmiConstants.HREF, name)) { // A link to an external resource
            Log.warn("External features are not supported yet");

            ignoreCurrentElement();
            return true;
        }

        if (nonNull(Strings.emptyToNull(prefix))) {
            final String prefixedValue = XmiConstants.format(prefix, name);

            if (Objects.equals(XmiConstants.XMI_IDREF, prefixedValue)) { // A reference of the previous element
                BasicReference reference = new BasicReference()
                        .name(getCurrentElement().name())
                        .owner(getPreviousId())
                        .stringValue(value);

                notifyReference(reference);

                ignoreCurrentElement();
                return true;
            }

            if (Objects.equals(XmiConstants.XMI_ID, prefixedValue)) { // The identifier of the current element
                getCurrentElement().rawId(value);

                return true;
            }

            if (Objects.equals(XmiConstants.XMI_VERSION_ATTR, prefixedValue)) { // The version of the read XMI file
                return true;
            }

            if (prefixedValue.matches(XmiConstants.XMI_XSI_TYPE)) { // The meta-class of the current element
                readMetaClass(value);

                return true;
            }
        }

        return false;
    }
}
