/*
    Copyright 2017 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.wolandsoft.sss.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds different implementation objects from the same interface allowing filtering them.
 *
 * @author Alexander Shulgin
 */
public class ListenerList<X> extends LinkedList<X> {

    public <T extends X> List<T> getCompatible(Class<T> clazz) {
        List<T> result = new LinkedList<>();
        for (X next : this) {
            if (clazz.isInstance(next)) {
                //noinspection unchecked
                result.add((T) next);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
