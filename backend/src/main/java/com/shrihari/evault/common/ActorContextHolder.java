package com.shrihari.evault.common;

public final class ActorContextHolder {

    private static final ThreadLocal<ActorContext> CONTEXT = new ThreadLocal<>();

    private ActorContextHolder() {
    }

    public static void set(ActorContext actorContext) {
        CONTEXT.set(actorContext);
    }

    public static ActorContext get() {
        ActorContext context = CONTEXT.get();
        return context != null ? context : ActorContext.system();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
