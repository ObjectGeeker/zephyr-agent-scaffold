package com.object.ai.agent.tools;

import com.google.adk.tools.Annotations;
import com.google.adk.tools.ToolContext;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FrameworkTools {

    @Annotations.Schema(name = "getCurrentTime", description = "获取当前系统时间")
    public static String getCurrentTime(@Annotations.Schema(name = "toolContext") ToolContext toolContext) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

}
