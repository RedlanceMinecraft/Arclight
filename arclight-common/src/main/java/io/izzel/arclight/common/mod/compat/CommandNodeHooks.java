package io.izzel.arclight.common.mod.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;

import java.util.Map;

public class CommandNodeHooks {

    private static final long CHILDREN, LITERALS, ARGUMENTS, CURRENT;
    private static final Object CURRENT_BASE;

    static {
        try {
            CHILDREN = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("children"));
            LITERALS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("literals"));
            ARGUMENTS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("arguments"));
            CURRENT_BASE = Unsafe.staticFieldBase(CommandNode.class.getDeclaredField("CURRENT_COMMAND"));
            CURRENT = Unsafe.staticFieldOffset(CommandNode.class.getDeclaredField("CURRENT_COMMAND"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeCommand(CommandNode<?> node, String command) {
        ((Map<String, ?>) Unsafe.getObject(node, CHILDREN)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, LITERALS)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, ARGUMENTS)).remove(command);
    }

    public static CommandNode<?> getCurrent() {
        return (CommandNode<?>) Unsafe.getObjectVolatile(CURRENT_BASE, CURRENT);
    }

    @SuppressWarnings("unchecked")
    public static <S> boolean canUse(CommandNode<S> node, S source) {
        return canUse(node, source, args -> ((CommandNode<S>) args[0]).canUse((S) args[1]));
    }

    public static <S> boolean canUse(CommandNode<S> node, S source, Operation<Boolean> original) {
        if (source instanceof CommandSourceBridge s) {
            try {
                s.bridge$setCurrentCommand(node);
                return original.call(node, source);
            } finally {
                s.bridge$setCurrentCommand(null);
            }
        } else {
            return original.call(node, source);
        }
    }
}
