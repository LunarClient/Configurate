/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.yaml;

import org.spongepowered.configurate.loader.CommentHandlers;

import java.util.stream.Stream;

final class YamlCommentHandler extends CommentHandlers.AbstractPrefixHandler {

    static final YamlCommentHandler INSTANCE = new YamlCommentHandler();

    private static final char PREFIX = '#';

    public static boolean applyPadding(final String line) {
        final char first;
        return !line.isEmpty() && ((first = line.charAt(0)) != ' ' && first != PREFIX && first != '-');
    }

    private YamlCommentHandler() {
        super(String.valueOf(PREFIX));
    }

    @Override
    public Stream<String> toComment(final Stream<String> lines) {
        return lines.map(line -> {
            if (!applyPadding(line)) {
                return this.commentPrefix + line;
            }

            return this.commentPrefix + ' ' + line;
        });
    }

}
