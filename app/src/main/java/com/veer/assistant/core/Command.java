package com.veer.assistant.core;
public final class Command {
    public final String action;
    public final String[] args;
    public final String raw;
    public Command(String a, String r, String... x) {
        this.action = a; this.raw = r; this.args = x == null ? new String[0] : x;
    }
    public String arg(int i) { return i < args.length ? args[i] : null; }
    @Override public String toString() { return action + java.util.Arrays.toString(args); }
}
