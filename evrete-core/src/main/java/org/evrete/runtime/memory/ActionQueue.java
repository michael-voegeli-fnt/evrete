package org.evrete.runtime.memory;

import org.evrete.api.Action;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiConsumer;

public class ActionQueue<T> {
    private final EnumMap<Action, Queue<T>> data = new EnumMap<>(Action.class);

    public ActionQueue() {
        for (Action action : Action.values()) {
            data.put(action, new LinkedList<>());
        }
    }

    public void add(Action action, T o) {
        data.get(action).add(o);
    }

    public void clear() {
        for (Queue<T> queue : data.values()) {
            queue.clear();
        }
    }

    public Queue<T> get(Action action) {
        return data.get(action);
    }

    public void forEach(BiConsumer<? super Action, ? super Queue<T>> action) {
        data.forEach(action);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
