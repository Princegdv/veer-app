package com.veer.assistant.plugins;
import android.content.Context;
import com.veer.assistant.core.Command;
public interface Plugin {
    String name();
    boolean canHandle(Command c);
    String handle(Context ctx, Command c) throws Exception;
}
