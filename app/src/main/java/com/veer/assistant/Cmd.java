package com.veer.assistant;

/** Ek samjha hua command: type + upto 3 arguments. Pure Java. */
final class Cmd {
    final String type;
    final String a;
    final String b;
    final String c;

    Cmd(String type) {
        this(type, null, null, null);
    }

    Cmd(String type, String a) {
        this(type, a, null, null);
    }

    Cmd(String type, String a, String b) {
        this(type, a, b, null);
    }

    Cmd(String type, String a, String b, String c) {
        this.type = type;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public String toString() {
        return type + "(" + a + "," + b + "," + c + ")";
    }
}
