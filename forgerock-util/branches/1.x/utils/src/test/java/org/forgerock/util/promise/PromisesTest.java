/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.util.promise;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

public class PromisesTest {

    @Test
    public void promiseCreatedByWhenShouldCompleteWithEmptyPromiseList() throws Exception {

        //Given
        final List<Promise<Void, NeverThrowsException>> promises =
                new ArrayList<Promise<Void, NeverThrowsException>>();
        final AtomicBoolean complete = new AtomicBoolean(false);

        //When
        Promises.when(promises)
                .thenAlways(new Runnable() {
                    @Override
                    public void run() {
                        complete.set(true);
                    }
                });

        //Then
        assertThat(complete.get()).describedAs("Promises.when did not complete").isTrue();
    }
}
