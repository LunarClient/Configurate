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
package org.spongepowered.configurate.transformation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of {@link ConfigurationTransformation}.
 *
 * <p>Transformations are executed from deepest in the configuration hierarchy
 * outwards.
 */
final class SingleConfigurationTransformation implements ConfigurationTransformation {

    private final MoveStrategy strategy;
    private final Map<NodePath, TransformAction> actions;

    /**
     * Thread local {@link NodePath} instance - used so we don't have to create
     * lots of NodePath instances.
     *
     * <p>As such, data within paths is only guaranteed to be the same during a
     * run of a transform function.
     */
    private final ThreadLocal<MutableNodePath> sharedPath = ThreadLocal.withInitial(MutableNodePath::new);

    SingleConfigurationTransformation(final Map<NodePath, TransformAction> actions, final MoveStrategy strategy) {
        this.actions = actions;
        this.strategy = strategy;
    }

    @Override
    public void apply(final ConfigurationNode node) throws ConfigurateException {
        @Nullable ConfigurateException thrown = null;
        for (Map.Entry<NodePath, TransformAction> ent : this.actions.entrySet()) {
            try {
                applySingleAction(node, ent.getKey().array(), 0, node, ent.getValue());
            } catch (final ConfigurateException ex) {
                if (thrown == null) {
                    thrown = ex;
                } else {
                    thrown.addSuppressed(ex);
                }
            }
        }

        if (thrown != null) {
            throw thrown;
        }
    }

    private void applySingleAction(final ConfigurationNode start, final Object[] path, final int startIdx, ConfigurationNode node,
            final TransformAction action) throws ConfigurateException {
        @Nullable ConfigurateException thrown = null;
        for (int i = startIdx; i < path.length; ++i) {
            if (path[i] == WILDCARD_OBJECT) {
                if (node.isList()) {
                    final List<? extends ConfigurationNode> children = node.childrenList();
                    for (int di = 0; di < children.size(); ++di) {
                        path[i] = di;
                        try {
                            applySingleAction(start, path, i + 1, children.get(di), action);
                        } catch (final ConfigurateException ex) {
                            if (thrown == null) {
                                thrown = ex;
                            } else {
                                thrown.addSuppressed(ex);
                            }
                        }
                    }
                    path[i] = WILDCARD_OBJECT;
                } else if (node.isMap()) {
                    for (Map.Entry<Object, ? extends ConfigurationNode> ent : node.childrenMap().entrySet()) {
                        path[i] = ent.getKey();
                        try {
                            applySingleAction(start, path, i + 1, ent.getValue(), action);
                        } catch (final ConfigurateException ex) {
                            if (thrown == null) {
                                thrown = ex;
                            } else {
                                thrown.addSuppressed(ex);
                            }
                        }
                    }
                    path[i] = WILDCARD_OBJECT;
                } else {
                    // No children
                    return;
                }
                return;
            } else {
                node = node.node(path[i]);
                if (node.virtual()) {
                    return;
                }
            }
        }

        // apply action
        final MutableNodePath nodePath = this.sharedPath.get();
        nodePath.arr = path;

        final Object @Nullable [] transformedPath = action.visitPath(nodePath, node);
        if (transformedPath != null && !Arrays.equals(path, transformedPath)) {
            this.strategy.move(node, start.node(transformedPath));
            node.remove();
        }
    }

}
