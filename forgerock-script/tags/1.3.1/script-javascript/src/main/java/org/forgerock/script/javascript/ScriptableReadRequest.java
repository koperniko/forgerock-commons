/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.javascript;

import org.forgerock.json.resource.ReadRequest;
import org.forgerock.script.scope.Parameter;
import org.mozilla.javascript.Wrapper;

/**
 * Provides a {@code Scriptable} wrapper for a {@code ReadRequest} object.
 */
class ScriptableReadRequest extends AbstractScriptableRequest implements Wrapper {

    private static final long serialVersionUID = 1L;

    /** The map being wrapped. */
    private final ReadRequest request;

    /**
     * Constructs a new scriptable wrapper around the specified list.
     *
     * @param request
     *            the request to be wrapped.
     * @throws NullPointerException
     *             if the specified map is {@code null}.
     */
    public ScriptableReadRequest(final Parameter parameter, final ReadRequest request) {
        super(parameter, request);
        // superfluous, as ReadRequest doesn't have any custom content/fields over AbstractRequest...
        this.request = request;
    }

    @Override
    public String getClassName() {
        return "ScriptableReadRequest";
    }

}
